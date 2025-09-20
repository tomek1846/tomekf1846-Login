package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium.SessionPremiumCheck;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Objects;

public class PremiumLoginListener extends PacketAdapter {

    private final SessionManager sessionManager = new SessionManager();
    private final ProtocolManager pm;
    private final Plugin plugin;
    private final ConnectionResolver connectionResolver;
    private final CryptoService cryptoService = new CryptoService();

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

                String keyEvent = connectionResolver.connKey(event, null);
                sessionManager.put(keyEvent, session);

                Object connection = connectionResolver.findConnectionFor(event);
                if (connection != null) {
                    try {
                        String keyConn = connectionResolver.connKey(event, connection);
                        if (!keyConn.equals(keyEvent)) sessionManager.put(keyConn, session);
                    } catch (Throwable ignored) {}
                }

                plugin.getLogger().info("[PremiumLogin] START username=" + username + " stored under keys (example) keyEvent=" + keyEvent
                        + (connection != null ? " (connection-based key also stored)" : " (no connection found now)"));

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
            Object connection = connectionResolver.findConnectionFor(event);
            String lookupKey = connectionResolver.connKey(event, connection);

            PremiumSession session = sessionManager.get(lookupKey);
            if (session == null) {
                String fallbackKey = connectionResolver.connKey(event, null);
                session = sessionManager.get(fallbackKey);
            }
            if (session == null) {
                String idKey = String.valueOf(System.identityHashCode(event));
                session = sessionManager.get(idKey);
            }

            boolean cancelEvent = false;
            try {
                byte[] encShared = event.getPacket().getByteArrays().read(0);
                byte[] encToken  = event.getPacket().getByteArrays().read(1);

                if (session == null) {
                    session = sessionManager.findSessionByDecrypting(encToken);
                    if (session != null) {
                        plugin.getLogger().info("[PremiumLogin] Session matched by decrypt fallback for connection " + lookupKey + " username=" + session.username);
                    }
                }

                if (session == null) {
                    plugin.getLogger().warning("[PremiumLogin] No session found for incoming ENCRYPTION_BEGIN (connection=" + lookupKey + "). Ignoring.");
                    return;
                }

                cancelEvent = true;

                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
                byte[] shared = rsa.doFinal(encShared);
                byte[] token  = rsa.doFinal(encToken);

                if (!java.util.Arrays.equals(token, session.verifyToken)) {
                    plugin.getLogger().warning("[PremiumLogin] Token mismatch for session username=" + session.username + " connection=" + lookupKey);
                    safeDisconnect(event, connection, "Failed to verify username! (token)");
                    sessionManager.removeSession(session);
                    return;
                }

                SecretKey secretKey = CryptoService.sharedSecretToKey(shared);
                session.sharedKey = secretKey;

                if (connection == null) {
                    plugin.getLogger().warning("[PremiumLogin] Nie udało się znaleźć Connection (ENCRYPTION_BEGIN).");
                    safeDisconnect(event, null, "Internal error (connection)");
                    sessionManager.removeSession(session);
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
                    sessionManager.removeSession(session);
                    return;
                }

                plugin.getLogger().info("[PremiumLogin] Calling sessionserver.hasJoined username=" + session.username + " serverHash=" + serverHash);

                MojangProfile profile = MojangAuthService.queryHasJoined(session.username, serverHash);
                if (profile == null) {
                    plugin.getLogger().warning("[PremiumLogin] hasJoined==null dla " + session.username + " (hash=" + serverHash + "). Wymagane premium -> rozłączam.");
                    safeDisconnect(event, connection, "§cTo konto wymaga logowania premium.\n§7Zaloguj się launcherem Mojang/Microsoft i spróbuj ponownie.");
                    sessionManager.removeSession(session);
                    return;
                }

                sessionManager.storeVerifiedProfile(lookupKey, profile);

                applyPremiumProfile(connection, profile, session.username);

                sessionManager.removeSession(session);

            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning("[PremiumLogin] Exception in ENCRYPTION_BEGIN: " + e.getMessage());
                try {
                    safeDisconnect(event, connection, "Failed to verify username!");
                } catch (Exception ex) {
                    plugin.getLogger().warning("[PremiumLogin] safeDisconnect failed: " + ex.getMessage());
                } finally {
                    try {
                        byte[] encToken = event.getPacket().getByteArrays().read(1);
                        PremiumSession ss = sessionManager.findSessionByDecrypting(encToken);
                        if (ss != null) sessionManager.removeSession(ss);
                    } catch (Throwable ignored) {}
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
        try {
            for (Class<?> current = connection.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {
                for (var field : current.getDeclaredFields()) {
                    if (!field.getType().getName().equals("io.netty.channel.Channel")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object channel = field.get(connection);
                    if (channel == null) {
                        continue;
                    }
                    try {
                        Method closeMethod = channel.getClass().getMethod("close");
                        closeMethod.setAccessible(true);
                        closeMethod.invoke(channel);
                        return true;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[PremiumLogin] Channel close failed: " + ex.getMessage());
        }
        return false;
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

    public MojangProfile consumeVerifiedProfile(String connKey) {
        return sessionManager.consumeVerifiedProfile(connKey);
    }

    public void clearSessions() {
        sessionManager.clearSessions();
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
