package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumLoginListener extends PacketAdapter {

    public static final String SPECIAL_NICK = "Tomekf1846";

    // map key -> session (może być kilka aliasów dla tej samej sesji)
    private final Map<String, PremiumSession> sessions = new ConcurrentHashMap<>();
    // map: connKey -> verified profile (zachowujemy dotychczasowe API consumeVerifiedProfile)
    private final Map<String, MojangProfile> verifiedProfiles = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ProtocolManager pm;
    private final Plugin plugin;

    public PremiumLoginListener(Plugin plugin, ProtocolManager pm) {
        super(plugin, ListenerPriority.HIGHEST,
                PacketType.Login.Client.START,
                PacketType.Login.Client.ENCRYPTION_BEGIN);
        this.pm = pm;
        this.plugin = plugin;
    }

    private static String socketKey(SocketAddress sa) {
        if (sa instanceof InetSocketAddress isa) {
            String host = isa.getAddress() != null ? isa.getAddress().getHostAddress() : isa.getHostString();
            return host + ":" + isa.getPort();
        }
        return sa != null ? sa.toString() : "null";
    }

    private String connKey(PacketEvent event, Object fallbackConnection) {
        try {
            Player p = event.getPlayer();
            if (p != null && p.getAddress() != null) {
                return socketKey(p.getAddress());
            }
        } catch (Exception ignored) {}

        if (fallbackConnection != null) {
            try {
                for (var f : fallbackConnection.getClass().getDeclaredFields()) {
                    if (f.getType().getName().equals("io.netty.channel.Channel")) {
                        f.setAccessible(true);
                        Object ch = f.get(fallbackConnection);
                        if (ch != null) {
                            Method remote = ch.getClass().getMethod("remoteAddress");
                            Object ra = remote.invoke(ch);
                            if (ra instanceof SocketAddress sa) {
                                return socketKey(sa);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return String.valueOf(System.identityHashCode(event));
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketType type = event.getPacketType();

        if (type.equals(PacketType.Login.Client.START)) {
            String username = event.getPacket().getStrings().read(0);
            if (!SPECIAL_NICK.equalsIgnoreCase(username)) return;

            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(1024);
                KeyPair kp = kpg.generateKeyPair();

                String serverId = "";
                byte[] verifyToken = new byte[4];
                random.nextBytes(verifyToken);

                PremiumSession session = new PremiumSession(username, serverId, verifyToken, kp);

                // zapisujemy sesję pod kilkoma aliasami (jeśli uda się je obliczyć)
                String keyEvent = connKey(event, null); // fallback - często identityHashCode(event)
                sessions.put(keyEvent, session);

                // spróbuj pobrać connection i zapisać również pod nim (jeśli uda)
                Object connection = findConnectionFor(event);
                if (connection != null) {
                    try {
                        String keyConn = connKey(event, connection);
                        if (!keyConn.equals(keyEvent)) sessions.put(keyConn, session);
                    } catch (Throwable ignored) {}
                }

                plugin.getLogger().info("[PremiumLogin] START username=" + username + " stored under keys (example) keyEvent=" + keyEvent
                        + (connection != null ? " (connection-based key also stored)" : " (no connection found now)"));

                // utwórz i wyślij EncryptionBegin
                PacketContainer req = pm.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                req.getStrings().write(0, serverId);
                req.getByteArrays().write(0, kp.getPublic().getEncoded());
                req.getByteArrays().write(1, verifyToken);

                try {
                    Player p = event.getPlayer();
                    if (p != null) {
                        pm.sendServerPacket(p, req);
                        plugin.getLogger().info("[PremiumLogin] Sent ENCRYPTION_BEGIN to Player object for " + username);
                    } else {
                        // event.getPlayer() null — spróbuj wysłać mimo to (ProtocolLib zwykle wymaga Player) -> logujemy i zostawiamy
                        plugin.getLogger().warning("[PremiumLogin] Player==null on START for username=" + username + " — ENCRYPTION_BEGIN wysłany do Player-a może nie przejść.");
                        // próbujemy jeszcze przez connection jeśli mamy
                        if (connection != null) {
                            // Nie gwarantuję, że ProtocolManager ma overload z raw connection w Twojej wersji — dlatego nie używam go tutaj,
                            // a jedynie loguję. Jeśli Twoja wersja ProtocolLib wspiera wysyłanie na raw connection, możesz dodać tu to wywołanie.
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
            Object connection = findConnectionFor(event);
            String lookupKey = connKey(event, connection);

            // first quick lookup attempts by plausible keys
            PremiumSession session = sessions.get(lookupKey);
            if (session == null) {
                String fallbackKey = connKey(event, null);
                session = sessions.get(fallbackKey);
            }
            if (session == null) {
                String idKey = String.valueOf(System.identityHashCode(event));
                session = sessions.get(idKey);
            }

            try {
                byte[] encShared = event.getPacket().getByteArrays().read(0);
                byte[] encToken  = event.getPacket().getByteArrays().read(1);

                // jeśli nie znaleziono sesji przez klucze — spróbuj znaleźć poprzez odszyfrowanie encToken każdą zapisaną sesją
                if (session == null) {
                    session = findSessionByDecrypting(encToken);
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
                    removeSession(session);
                    return;
                }

                SecretKey secretKey = CryptoUtil.sharedSecretToKey(shared);
                session.sharedKey = secretKey;

                if (connection == null) {
                    plugin.getLogger().warning("[PremiumLogin] Nie udało się znaleźć Connection (ENCRYPTION_BEGIN).");
                    safeDisconnect(event, "Internal error (connection)");
                    removeSession(session);
                    return;
                }

                // Setup szyfrowania po stronie NMS/connection
                try {
                    invokeSetupEncryption(connection, secretKey);
                } catch (NoSuchMethodException nsme) {
                    plugin.getLogger().warning("[PremiumLogin] Brak metody setupEncryption(SecretKey) - " + nsme.getMessage());
                    // mimo braku setupEncryption możemy spróbować dalej (ale raczej nie przejdzie)
                }

                // compute serverHash i query HasJoined
                String serverHash;
                try {
                    serverHash = MojangAuthService.computeServerHash(shared, session.keyPair.getPublic());
                } catch (Throwable t) {
                    // log i fail safe
                    plugin.getLogger().warning("[PremiumLogin] computeServerHash failed: " + t.getMessage());
                    t.printStackTrace();
                    safeDisconnect(event, "Failed to verify username!");
                    removeSession(session);
                    return;
                }

                plugin.getLogger().info("[PremiumLogin] Calling sessionserver.hasJoined username=" + session.username + " serverHash=" + serverHash);

                MojangProfile profile = MojangAuthService.queryHasJoined(session.username, serverHash);
                if (profile == null) {
                    plugin.getLogger().warning("[PremiumLogin] Mojang session server returned null profile for username=" + session.username + " serverHash=" + serverHash);
                    safeDisconnect(event, "Failed to verify username!");
                    removeSession(session);
                    return;
                }

                // store verified profile under any key that references this session (so consumeVerifiedProfile works)
                String sessionKey = getAnyKeyForSession(session);
                if (sessionKey == null) {
                    // awaryjnie generujemy tymczasowy klucz
                    sessionKey = "sess:" + System.identityHashCode(session);
                }
                verifiedProfiles.put(sessionKey, profile);

                // PODMIANA: ustaw GameProfile w handlerze logowania przed READY_TO_ACCEPT
                try {
                    Object loginHandler = extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
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
                removeSession(session);

            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning("[PremiumLogin] Exception in ENCRYPTION_BEGIN: " + e.getMessage());
                try {
                    safeDisconnect(event, "Failed to verify username!");
                } catch (Exception ex) {
                    plugin.getLogger().warning("[PremiumLogin] safeDisconnect failed: " + ex.getMessage());
                } finally {
                    // próbujemy znaleźć jakąkolwiek sesję powiązaną i usunąć ją
                    try {
                        byte[] encToken = event.getPacket().getByteArrays().read(1);
                        PremiumSession ss = findSessionByDecrypting(encToken);
                        if (ss != null) removeSession(ss);
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

    private Object findConnectionFor(PacketEvent event) {
        try {
            Player p = event.getPlayer();
            if (p == null || p.getAddress() == null) return null;
            InetSocketAddress target = p.getAddress();

            Object craftServer = Bukkit.getServer();
            Method getServer = craftServer.getClass().getMethod("getServer");
            Object mcServer = getServer.invoke(craftServer);
            if (mcServer == null) return null;

            Object scl = null;
            try {
                Method m = mcServer.getClass().getMethod("getConnection");
                if (m.getParameterCount() == 0 && !m.getReturnType().equals(void.class)) {
                    scl = m.invoke(mcServer);
                }
            } catch (NoSuchMethodException ignored) {}
            if (scl == null) {
                scl = extractFieldType(mcServer, "net.minecraft.server.network.ServerConnectionListener");
                if (scl == null) {
                    scl = extractFieldType(mcServer, "net.minecraft.server.network.ServerConnection");
                }
            }
            if (scl == null) return null;

            List<Object> connections = new ArrayList<>();
            collectByFqcn(scl, "net.minecraft.network.Connection", 0, new IdentityHashMap<>(), connections);

            for (Object conn : connections) {
                SocketAddress addr = null;
                for (String methodName : new String[] {"getRemoteAddress", "getSocketAddress"}) {
                    try {
                        Method m = conn.getClass().getMethod(methodName);
                        if (m.getParameterCount() == 0) {
                            Object v = m.invoke(conn);
                            if (v instanceof SocketAddress sa) {
                                addr = sa;
                                break;
                            }
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
                if (addr == null) {
                    try {
                        for (var f : conn.getClass().getDeclaredFields()) {
                            if (f.getType().getName().equals("io.netty.channel.Channel")) {
                                f.setAccessible(true);
                                Object ch = f.get(conn);
                                if (ch != null) {
                                    try {
                                        Method remote = ch.getClass().getMethod("remoteAddress");
                                        Object ra = remote.invoke(ch);
                                        if (ra instanceof SocketAddress sa) {
                                            addr = sa;
                                            break;
                                        }
                                    } catch (NoSuchMethodException ignored) {}
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                if (addr instanceof InetSocketAddress isa) {
                    String a = socketKey(isa);
                    String b = socketKey(target);
                    if (a.equals(b)) {
                        return conn;
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[PremiumLogin] Nie udało się pobrać Connection: " + t.getMessage());
            t.printStackTrace();
        }
        return null;
    }

    private static Object extractFieldType(Object obj, String fqcnStartsWith) {
        if (obj == null) return null;
        for (java.lang.reflect.Field f : obj.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v != null && v.getClass().getName().startsWith(fqcnStartsWith)) return v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void collectByFqcn(Object obj, String fqcnPrefix, int depth, IdentityHashMap<Object, Boolean> seen, List<Object> out) {
        if (obj == null || depth > 8 || seen.containsKey(obj)) return;
        seen.put(obj, Boolean.TRUE);

        Class<?> c = obj.getClass();
        if (c.getName().startsWith(fqcnPrefix)) {
            out.add(obj);
            return;
        }

        if (c.isArray()) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object v = Array.get(obj, i);
                collectByFqcn(v, fqcnPrefix, depth + 1, seen, out);
            }
            return;
        }

        if (obj instanceof Iterable<?>) {
            for (Object v : (Iterable<?>) obj) {
                collectByFqcn(v, fqcnPrefix, depth + 1, seen, out);
            }
        }
        if (obj instanceof Map<?, ?>) {
            for (Object v : ((Map<?, ?>) obj).values()) {
                collectByFqcn(v, fqcnPrefix, depth + 1, seen, out);
            }
        }

        for (java.lang.reflect.Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                collectByFqcn(v, fqcnPrefix, depth + 1, seen, out);
            } catch (Throwable ignored) {}
        }
    }

    private void invokeSetupEncryption(Object connection, SecretKey secretKey) throws Exception {
        Method m = null;
        try {
            m = connection.getClass().getMethod("setupEncryption", SecretKey.class);
        } catch (NoSuchMethodException ignored) {}

        if (m == null) {
            for (Method mm : connection.getClass().getMethods()) {
                if (mm.getParameterCount() == 1 && SecretKey.class.isAssignableFrom(mm.getParameterTypes()[0])) {
                    m = mm;
                    break;
                }
            }
        }

        if (m == null) {
            throw new NoSuchMethodException("Brak setupEncryption(SecretKey) w " + connection.getClass().getName());
        }

        m.setAccessible(true);
        m.invoke(connection, secretKey);
    }

    public MojangProfile consumeVerifiedProfile(String connKey) {
        return verifiedProfiles.remove(connKey);
    }

    public void clearSessions() {
        sessions.clear();
        verifiedProfiles.clear();
    }

    // ---- pomocnicze metody ----

    /**
     * Przeszukuje wszystkie zapisane sesje i próbuje odszyfrować encToken przy ich pomocy.
     * Jeśli odszyfrowanie się uda i porównanie tokenów pasuje -> zwracamy tę sesję.
     */
    private PremiumSession findSessionByDecrypting(byte[] encToken) {
        for (Map.Entry<String, PremiumSession> e : sessions.entrySet()) {
            PremiumSession s = e.getValue();
            try {
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, s.keyPair.getPrivate());
                byte[] token = rsa.doFinal(encToken);
                if (java.util.Arrays.equals(token, s.verifyToken)) {
                    return s;
                }
            } catch (Throwable ignored) {
                // nie pasuje - kontynuuj
            }
        }
        return null;
    }

    /**
     * Usuń daną sesję spod wszystkich kluczy w mapie sessions (cleanup).
     */
    private void removeSession(PremiumSession session) {
        if (session == null) return;
        Iterator<Map.Entry<String, PremiumSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PremiumSession> e = it.next();
            if (e.getValue() == session) {
                it.remove();
            }
        }
        // usuń też potencjalny verifiedProfile powiązany (jeśli było)
        Iterator<Map.Entry<String, MojangProfile>> itv = verifiedProfiles.entrySet().iterator();
        while (itv.hasNext()) {
            Map.Entry<String, MojangProfile> e = itv.next();
            // nie mamy bezpośredniego odwołania do sesji w profilu - zostawimy profile jeśli jest potrzeba
            // (opcjonalnie usunąć profile powiązane z tą sesją - ale nie mamy powiązania)
            // zostawiamy jak jest, by nie zniszczyć API consumeVerifiedProfile
            break;
        }
    }

    /**
     * Zwraca przykładowy (jeden) klucz mapy sessions pod którym przechowywana była dana sesja.
     * Przydatne do przechowywania verifiedProfiles pod tym kluczem.
     */
    private String getAnyKeyForSession(PremiumSession session) {
        for (Map.Entry<String, PremiumSession> e : sessions.entrySet()) {
            if (e.getValue() == session) return e.getKey();
        }
        return null;
    }
}
