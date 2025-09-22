package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium.SessionPremiumCheck;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class PremiumLoginListener extends PacketAdapter {
    private static final long SESSION_EXPIRY_MILLIS = 30_000L;

    private final ProtocolManager pm;
    private final Plugin plugin;
    private final ConnectionResolver connectionResolver;
    private final CryptoService cryptoService = new CryptoService();
    private final ConcurrentMap<String, ConcurrentLinkedQueue<PremiumSession>> pendingSessions = new ConcurrentHashMap<>();

    public PremiumLoginListener(Plugin plugin, ProtocolManager pm) {
        super(plugin, ListenerPriority.HIGHEST,
                PacketType.Login.Client.START,
                PacketType.Login.Client.ENCRYPTION_BEGIN);
        this.pm = pm;
        this.plugin = plugin;
        this.connectionResolver = new ConnectionResolver(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketType type = event.getPacketType();

        if (type.equals(PacketType.Login.Client.START)) {
            String username = event.getPacket().getStrings().read(0);
            if (!shouldHandlePremiumLogin(username)) return;

            try {
                PremiumSession session = cryptoService.createSession(username);
                registerPendingSession(session);

                Object connection = connectionResolver.findConnectionFor(event);
                Channel channel = connectionResolver.resolveChannel(event, connection);
                if (channel != null && (connection == null || connectionResolver.findChannel(connection) != channel)) {
                    Object byChannel = connectionResolver.findConnectionByChannel(channel);
                    if (byChannel != null) {
                        connection = byChannel;
                    }
                }
                String connKey = connectionResolver.connKey(event, connection);
                if (channel != null) {
                    channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(session);
                    channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(connection);
                    ensureCloseCleanup(channel);
                    plugin.getLogger().info("[PremiumLogin] START username=" + username + " bound premium session to channel "
                            + channel.remoteAddress() + " (key=" + connKey + ")");
                } else {
                    plugin.getLogger().warning("[PremiumLogin] START username=" + username + " - unable to resolve Netty channel (key="
                            + connKey + "). Falling back to pending session queue.");
                }

                PacketContainer req = pm.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                req.getStrings().write(0, session.serverId);
                req.getByteArrays().write(0, session.keyPair.getPublic().getEncoded());
                req.getByteArrays().write(1, session.verifyToken);

                try {
                    Player p = event.getPlayer();
                    if (p != null) {
                        pm.sendServerPacket(p, req);
                        plugin.getLogger().info("[PremiumLogin] Sent ENCRYPTION_BEGIN to Player object for " + username);
                    } else {
                        plugin.getLogger().warning("[PremiumLogin] Player==null on START for username=" + username + " — ENCRYPTION_BEGIN wysłany do Player-a może nie przejść.");
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("[PremiumLogin] Error while sending ENCRYPTION_BEGIN: " + ex.getMessage());
                    ex.printStackTrace();
                }

            } catch (Exception ex) {
                plugin.getLogger().warning("[PremiumLogin] START handler failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        else if (type.equals(PacketType.Login.Client.ENCRYPTION_BEGIN)) {
            Channel channel = connectionResolver.resolveChannel(event, null);
            Object connection = channel != null ? channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).get() : null;
            if (connection == null) {
                connection = connectionResolver.findConnectionFor(event);
            }
            if (channel != null && (connection == null || connectionResolver.findChannel(connection) != channel)) {
                Object byChannel = connectionResolver.findConnectionByChannel(channel);
                if (byChannel != null) {
                    connection = byChannel;
                    channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(connection);
                }
            }
            String lookupKey = connectionResolver.connKey(event, connection);

            Player tempPlayer;
            String username = null;
            try {
                tempPlayer = event.getPlayer();
                if (tempPlayer != null) {
                    username = tempPlayer.getName();
                }
            } catch (Exception ignored) {
            }

            boolean cancelEvent = false;
            PremiumSession session = null;
            try {
                byte[] encShared = event.getPacket().getByteArrays().read(0);
                byte[] encToken  = event.getPacket().getByteArrays().read(1);

                session = resolvePendingSession(channel, username, encToken);
                if (session == null) {
                    plugin.getLogger().warning("[PremiumLogin] No premium session bound to connection " + lookupKey + ". Ignoring.");
                    event.setCancelled(true);
                    safeDisconnect(event, connection, "Failed to verify username!");
                    clearSession(channel);
                    discardPendingSessions(username);
                    return;
                }

                cancelEvent = true;

                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
                byte[] shared = rsa.doFinal(encShared);
                byte[] token  = rsa.doFinal(encToken);

                if (!Arrays.equals(token, session.verifyToken)) {
                    plugin.getLogger().warning("[PremiumLogin] Token mismatch for session username=" + session.username + " connection=" + lookupKey);
                    safeDisconnect(event, connection, "Failed to verify username! (token)");
                    clearSession(channel);
                    removePendingSession(session);
                    return;
                }

                SecretKey secretKey = CryptoService.sharedSecretToKey(shared);
                session.sharedKey = secretKey;

                if (connection == null) {
                    plugin.getLogger().warning("[PremiumLogin] Nie udało się znaleźć Connection (ENCRYPTION_BEGIN).");
                    safeDisconnect(event, null, "Internal error (connection)");
                    clearSession(channel);
                    removePendingSession(session);
                    return;
                }

                try {
                    connectionResolver.invokeSetupEncryption(connection, secretKey);
                } catch (NoSuchMethodException nsme) {
                    plugin.getLogger().warning("[PremiumLogin] Brak metody setupEncryption(SecretKey) - " + nsme.getMessage());
                }

                String serverHash;
                try {
                    serverHash = MojangAuthService.computeServerHash(shared, session.keyPair.getPublic());
                } catch (Throwable t) {
                    plugin.getLogger().warning("[PremiumLogin] computeServerHash failed: " + t.getMessage());
                    t.printStackTrace();
                    safeDisconnect(event, connection, "Failed to verify username!");
                    clearSession(channel);
                    removePendingSession(session);
                    return;
                }

                plugin.getLogger().info("[PremiumLogin] Calling sessionserver.hasJoined username=" + session.username + " serverHash=" + serverHash);

                MojangProfile profile = MojangAuthService.queryHasJoined(session.username, serverHash);
                if (profile == null) {
                    plugin.getLogger().warning("[PremiumLogin] hasJoined==null dla " + session.username + " (hash=" + serverHash + "). Wymagane premium -> rozłączam.");
                    safeDisconnect(event, connection, "§cTo konto wymaga logowania premium.\n§7Zaloguj się launcherem Mojang/Microsoft i spróbuj ponownie.");
                    clearSession(channel);
                    removePendingSession(session);
                    return;
                }

                Objects.requireNonNull(channel).attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(profile);

                applyPremiumProfile(connection, profile, session.username);

                clearSession(channel);

            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning("[PremiumLogin] Exception in ENCRYPTION_BEGIN: " + e.getMessage());
                try {
                    safeDisconnect(event, connection, "Failed to verify username!");
                } catch (Exception ex) {
                    plugin.getLogger().warning("[PremiumLogin] safeDisconnect failed: " + ex.getMessage());
                } finally {
                    removePendingSession(session);
                    clearSession(channel);
                }
            } finally {
                if (cancelEvent) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean shouldHandlePremiumLogin(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (PlayerDataSave.isPlayerPremium(username)) {
            return true;
        }

        boolean premiumCommandEnabled = plugin.getConfig().getBoolean("Main-Settings.Premium-Command", false);
        if (!premiumCommandEnabled) {
            return SessionPremiumCheck.isPlayerPremium(username);
        }

        return false;
    }

    private void safeDisconnect(PacketEvent event, Object connection, String msg) {
        boolean disconnected = false;
        try {
            PacketContainer dis = pm.createPacket(PacketType.Login.Server.DISCONNECT);
            dis.getChatComponents().write(0, WrappedChatComponent.fromText(msg));
            Player p = event.getPlayer();
            if (p != null) {
                pm.sendServerPacket(p, dis);
                disconnected = true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PremiumLogin] Failed to send disconnect packet: " + e.getMessage());
        }

        if (disconnected) {
            return;
        }

        if (connection == null) {
            try {
                connection = connectionResolver.findConnectionFor(event);
            } catch (Exception ignored) {
            }
        }

        if (connection == null) {
            plugin.getLogger().warning("[PremiumLogin] Nie można rozłączyć połączenia (brak referencji connection). Msg: " + msg);
            return;
        }

        if (invokeDisconnectOnLoginHandler(connection, msg)) {
            return;
        }

        if (invokeDisconnect(connection, msg)) {
            return;
        }

        if (closeChannel(connection)) {
            return;
        }

        plugin.getLogger().warning("[PremiumLogin] Nie udało się rozłączyć gracza mimo prób fallback. Msg: " + msg);
    }

    private boolean invokeDisconnectOnLoginHandler(Object connection, String msg) {
        Object loginHandler = ConnectionResolver.extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
        if (loginHandler != null) {
            return invokeDisconnect(loginHandler, msg);
        }
        return false;
    }

    private boolean invokeDisconnect(Object target, String msg) {
        if (target == null) {
            return false;
        }

        ChatComponentWrapper component = ChatComponentWrapper.fromText(msg);
        if (component == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals("disconnect") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!paramType.isInstance(component.instance())) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(target, component.instance());
                return true;
            } catch (Exception ex) {
                plugin.getLogger().warning("[PremiumLogin] disconnect() invocation failed: " + ex.getMessage());
                return false;
            }
        }

        return false;
    }

    private boolean closeChannel(Object connection) {
        if (connection == null) {
            return false;
        }

        try {
            Channel channel = connectionResolver.findChannel(connection);
            if (channel == null) {
                return false;
            }
            channel.close();
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("[PremiumLogin] Channel close failed: " + ex.getMessage());
        }
        return false;
    }

    private void ensureCloseCleanup(Channel channel) {
        if (channel == null) {
            return;
        }

        Boolean attached = channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).get();
        if (Boolean.TRUE.equals(attached)) {
            return;
        }

        channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).set(Boolean.TRUE);
        channel.closeFuture().addListener(future -> clearChannelState(channel));
    }

    private void clearChannelState(Channel channel) {
        if (channel == null) {
            return;
        }

        clearSession(channel);
        channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(null);
        channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(null);
        channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).set(null);
    }

    private void registerPendingSession(PremiumSession session) {
        if (session == null || session.username == null || session.username.isBlank()) {
            return;
        }

        String key = session.username.toLowerCase(Locale.ROOT);
        pendingSessions.compute(key, (k, queue) -> {
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }
            queue.add(session);
            return queue;
        });
    }

    private PremiumSession resolvePendingSession(Channel channel, String username, byte[] encToken) {
        PremiumSession fromChannel = channel != null ? channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).get() : null;
        if (fromChannel != null) {
            removePendingSession(fromChannel);
            ensureCloseCleanup(channel);
            return fromChannel;
        }

        PremiumSession matched = pollPendingSession(username, encToken);
        if (matched != null && channel != null) {
            channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(matched);
            ensureCloseCleanup(channel);
        }
        return matched;
    }

    private PremiumSession pollPendingSession(String username, byte[] encToken) {
        if (encToken == null) {
            return null;
        }

        if (username != null && !username.isBlank()) {
            String key = username.toLowerCase(Locale.ROOT);
            PremiumSession session = pollPendingSessionFromQueue(key, pendingSessions.get(key), encToken);
            if (session != null) {
                return session;
            }
        }

        return pollPendingSessionFromAllQueues(encToken);
    }

    private PremiumSession pollPendingSessionFromQueue(String key, Queue<PremiumSession> queue, byte[] encToken) {
        if (queue == null) {
            return null;
        }

        PremiumSession matched = null;
        long now = System.currentTimeMillis();
        for (Iterator<PremiumSession> it = queue.iterator(); it.hasNext();) {
            PremiumSession candidate = it.next();
            if (matchesVerifyToken(candidate, encToken)) {
                matched = candidate;
                it.remove();
                break;
            }

            if (now - candidate.createdAt > SESSION_EXPIRY_MILLIS) {
                it.remove();
            }
        }

        if (queue.isEmpty()) {
            pendingSessions.remove(key, queue);
        }

        return matched;
    }

    private PremiumSession pollPendingSessionFromAllQueues(byte[] encToken) {
        for (Map.Entry<String, ConcurrentLinkedQueue<PremiumSession>> entry : pendingSessions.entrySet()) {
            PremiumSession matched = pollPendingSessionFromQueue(entry.getKey(), entry.getValue(), encToken);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    private boolean matchesVerifyToken(PremiumSession session, byte[] encToken) {
        if (session == null || encToken == null) {
            return false;
        }

        try {
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
            byte[] token = rsa.doFinal(encToken);
            return Arrays.equals(token, session.verifyToken);
        } catch (Exception ignored) {
        }
        return false;
    }

    private void removePendingSession(PremiumSession session) {
        if (session == null || session.username == null || session.username.isBlank()) {
            return;
        }

        String key = session.username.toLowerCase(Locale.ROOT);
        Queue<PremiumSession> queue = pendingSessions.get(key);
        if (queue == null) {
            return;
        }

        queue.remove(session);
        if (queue.isEmpty()) {
            pendingSessions.remove(key, queue);
        }
    }

    private void discardPendingSessions(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String key = username.toLowerCase(Locale.ROOT);
        Queue<PremiumSession> queue = pendingSessions.remove(key);
        if (queue != null) {
            queue.clear();
        }
    }

    private void clearSession(Channel channel) {
        if (channel == null) {
            return;
        }

        PremiumSession session = channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).get();
        if (session != null) {
            removePendingSession(session);
        }
        channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(null);
    }

    private record ChatComponentWrapper(Object instance) {

        public static ChatComponentWrapper fromText(String text) {
            try {
                Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
                Method literal = componentClass.getMethod("literal", String.class);
                Object comp = literal.invoke(null, text);
                return new ChatComponentWrapper(comp);
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                    Class<?> serializer = Class.forName("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
                    Method a = serializer.getMethod("a", String.class);
                    String json = Objects.toString(text, "")
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n");
                    Object comp = a.invoke(null, "{\"text\":\"" + json + "\"}");
                    return new ChatComponentWrapper(comp);
                } catch (Exception ignored) {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public MojangProfile consumeVerifiedProfile(PacketEvent event) {
        Object connection = connectionResolver.findConnectionFor(event);
        Channel channel = connectionResolver.resolveChannel(event, connection);
        if (channel == null) {
            return null;
        }

        MojangProfile profile = channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).get();
        if (profile != null) {
            channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(null);
        }
        return profile;
    }

    public void clearSessions() {
        pendingSessions.clear();
    }

    private void applyPremiumProfile(Object connection, MojangProfile profile, String username) {
        try {
            Object loginHandler = ConnectionResolver.extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
            if (loginHandler != null) {
                LoginStateUtil.setLoginGameProfile(loginHandler, profile);
                LoginStateUtil.setReadyToAccept(loginHandler);
                plugin.getLogger().info("[PremiumLogin] Set GameProfile and READY_TO_ACCEPT for username=" + username);
            } else {
                plugin.getLogger().warning("[PremiumLogin] Nie znaleziono loginHandler (ServerLoginPacketListenerImpl) do ustawienia profilu.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[PremiumLogin] Nie udało się ustawić profilu/READY_TO_ACCEPT: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
