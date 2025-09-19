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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Method;

public class PremiumLoginListener extends PacketAdapter {

    public static final String SPECIAL_NICK = "Tomekf1846";

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
            if (!SPECIAL_NICK.equalsIgnoreCase(username)) return;

            try {
                PremiumSession session = cryptoService.createSession(username);

                // zapisujemy sesję pod kilkoma aliasami (jeśli uda się je obliczyć)
                String keyEvent = connectionResolver.connKey(event, null); // fallback - często identityHashCode(event)
                sessionManager.put(keyEvent, session);

                // spróbuj pobrać connection i zapisać również pod nim (jeśli uda)
                Object connection = connectionResolver.findConnectionFor(event);
                if (connection != null) {
                    try {
                        String keyConn = connectionResolver.connKey(event, connection);
                        if (!keyConn.equals(keyEvent)) sessionManager.put(keyConn, session);
                    } catch (Throwable ignored) {}
                }

                plugin.getLogger().info("[PremiumLogin] START username=" + username + " stored under keys (example) keyEvent=" + keyEvent
                        + (connection != null ? " (connection-based key also stored)" : " (no connection found now)"));

                // utwórz i wyślij EncryptionBegin
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
                        if (connection != null) {
                            // jak w oryginale: logujemy i zostawiamy (nie wywołujemy surowego wysyłania)
                        }
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

            // first quick lookup attempts by plausible keys
            PremiumSession session = sessionManager.get(lookupKey);
            if (session == null) {
                String fallbackKey = connectionResolver.connKey(event, null);
                session = sessionManager.get(fallbackKey);
            }
            if (session == null) {
                String idKey = String.valueOf(System.identityHashCode(event));
                session = sessionManager.get(idKey);
            }

            try {
                byte[] encShared = event.getPacket().getByteArrays().read(0);
                byte[] encToken  = event.getPacket().getByteArrays().read(1);

                // jeśli nie znaleziono sesji przez klucze — spróbuj znaleźć poprzez odszyfrowanie encToken każdą zapisaną sesją
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

                // odszyfruj shared i token przy użyciu klucza prywatnego danej sesji
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
                byte[] shared = rsa.doFinal(encShared);
                byte[] token  = rsa.doFinal(encToken);

                if (!java.util.Arrays.equals(token, session.verifyToken)) {
                    plugin.getLogger().warning("[PremiumLogin] Token mismatch for session username=" + session.username + " connection=" + lookupKey);
                    safeDisconnect(event, "Failed to verify username! (token)");
                    sessionManager.removeSession(session);
                    return;
                }

                SecretKey secretKey = CryptoService.sharedSecretToKey(shared);
                session.sharedKey = secretKey;

                if (connection == null) {
                    plugin.getLogger().warning("[PremiumLogin] Nie udało się znaleźć Connection (ENCRYPTION_BEGIN).");
                    safeDisconnect(event, "Internal error (connection)");
                    sessionManager.removeSession(session);
                    return;
                }

                // Setup szyfrowania po stronie NMS/connection
                try {
                    connectionResolver.invokeSetupEncryption(connection, secretKey);
                } catch (NoSuchMethodException nsme) {
                    plugin.getLogger().warning("[PremiumLogin] Brak metody setupEncryption(SecretKey) - " + nsme.getMessage());
                    // mimo braku setupEncryption możemy spróbować dalej (ale raczej nie przejdzie)
                }

                // compute serverHash i query HasJoined
                String serverHash;
                try {
                    serverHash = MojangAuthService.computeServerHash(shared, session.keyPair.getPublic());
                } catch (Throwable t) {
                    plugin.getLogger().warning("[PremiumLogin] computeServerHash failed: " + t.getMessage());
                    t.printStackTrace();
                    safeDisconnect(event, "Failed to verify username!");
                    sessionManager.removeSession(session);
                    return;
                }

                plugin.getLogger().info("[PremiumLogin] Calling sessionserver.hasJoined username=" + session.username + " serverHash=" + serverHash);

                MojangProfile profile = MojangAuthService.queryHasJoined(session.username, serverHash);
                if (profile == null) {
                    plugin.getLogger().warning("[PremiumLogin] Mojang session server returned null profile for username=" + session.username + " serverHash=" + serverHash);
                    safeDisconnect(event, "Failed to verify username!");
                    sessionManager.removeSession(session);
                    return;
                }

                // store verified profile under any key that references this session (so consumeVerifiedProfile works)
                String sessionKey = sessionManager.getAnyKeyForSession(session);
                if (sessionKey == null) {
                    sessionKey = "sess:" + System.identityHashCode(session);
                }
                sessionManager.storeVerifiedProfile(sessionKey, profile);

                // PODMIANA: ustaw GameProfile w handlerze logowania przed READY_TO_ACCEPT
                try {
                    Object loginHandler = ConnectionResolver.extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
                    if (loginHandler != null) {
                        // najpierw ustaw profil, potem ready
                        LoginStateUtil.setLoginGameProfile(loginHandler, profile);
                        LoginStateUtil.setReadyToAccept(loginHandler);
                        plugin.getLogger().info("[PremiumLogin] Set GameProfile and READY_TO_ACCEPT for username=" + session.username);
                    } else {
                        plugin.getLogger().warning("[PremiumLogin] Nie znaleziono loginHandler (ServerLoginPacketListenerImpl) do ustawienia profilu.");
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[PremiumLogin] Nie udało się ustawić profilu/READY_TO_ACCEPT: " + t.getMessage());
                    t.printStackTrace();
                }

                // konsumuj pakiet klienta (niech dalej login flow pójdzie z podmienionym profilem)
                event.setCancelled(true);

                // nie usuwamy od razu sesji - w razie potrzeby możemy później jej użyć (ale lepiej usunąć)
                sessionManager.removeSession(session);

            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning("[PremiumLogin] Exception in ENCRYPTION_BEGIN: " + e.getMessage());
                try {
                    safeDisconnect(event, "Failed to verify username!");
                } catch (Exception ex) {
                    plugin.getLogger().warning("[PremiumLogin] safeDisconnect failed: " + ex.getMessage());
                } finally {
                    try {
                        byte[] encToken = event.getPacket().getByteArrays().read(1);
                        PremiumSession ss = sessionManager.findSessionByDecrypting(encToken);
                        if (ss != null) sessionManager.removeSession(ss);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    /**
     * Bezpieczne wysłanie pakietu rozłączenia — nie rzuca wyjątków na zewnątrz.
     */
    private void safeDisconnect(PacketEvent event, String msg) {
        try {
            PacketContainer dis = pm.createPacket(PacketType.Login.Server.DISCONNECT);
            dis.getChatComponents().write(0, WrappedChatComponent.fromText(msg));
            Player p = event.getPlayer();
            if (p != null) {
                pm.sendServerPacket(p, dis);
            } else {
                plugin.getLogger().warning("[PremiumLogin] Nie można wysłać DISCONNECT, event.getPlayer() == null. Msg: " + msg);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PremiumLogin] Failed to send disconnect: " + e.getMessage());
        }
    }

    // zachowujemy API (delegacja)
    public MojangProfile consumeVerifiedProfile(String connKey) {
        return sessionManager.consumeVerifiedProfile(connKey);
    }

    public void clearSessions() {
        sessionManager.clearSessions();
    }
}
