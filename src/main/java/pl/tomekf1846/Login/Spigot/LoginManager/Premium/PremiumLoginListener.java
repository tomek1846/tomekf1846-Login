package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
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

    private final Map<String, PremiumSession> sessions = new ConcurrentHashMap<>();
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

                String key = connKey(event, null);
                sessions.put(key, new PremiumSession(username, serverId, verifyToken, kp));

                PacketContainer req = pm.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                req.getStrings().write(0, serverId);
                req.getByteArrays().write(0, kp.getPublic().getEncoded());
                req.getByteArrays().write(1, verifyToken);
                pm.sendServerPacket(event.getPlayer(), req);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        else if (type.equals(PacketType.Login.Client.ENCRYPTION_BEGIN)) {
            Object connection = findConnectionFor(event);
            String key = connKey(event, connection);
            PremiumSession session = sessions.get(key);
            if (session == null) return;

            try {
                byte[] encShared = event.getPacket().getByteArrays().read(0);
                byte[] encToken  = event.getPacket().getByteArrays().read(1);

                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
                byte[] shared = rsa.doFinal(encShared);
                byte[] token  = rsa.doFinal(encToken);

                if (!java.util.Arrays.equals(token, session.verifyToken)) {
                    disconnect(event, "Failed to verify username! (token)");
                    event.setCancelled(true);
                    sessions.remove(key);
                    return;
                }

                SecretKey secretKey = CryptoUtil.sharedSecretToKey(shared);
                session.sharedKey = secretKey;

                if (connection == null) {
                    plugin.getLogger().warning("[tomekf1846-Login] Nie udało się znaleźć Connection");
                    disconnect(event, "Internal error (connection)");
                    event.setCancelled(true);
                    sessions.remove(key);
                    return;
                }
                invokeSetupEncryption(connection, secretKey);

                String serverHash = MojangAuthService.computeServerHash(shared, session.keyPair.getPublic());
                MojangProfile profile = MojangAuthService.queryHasJoined(session.username, serverHash);
                if (profile == null) {
                    disconnect(event, "Failed to verify username!");
                    event.setCancelled(true);
                    sessions.remove(key);
                    return;
                }

                verifiedProfiles.put(key, profile);

                // PODMIANA: ustaw GameProfile w handlerze logowania przed READY_TO_ACCEPT
                try {
                    Object loginHandler = extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
                    if (loginHandler != null) {
                        // najpierw ustaw profil, potem ready
                        LoginStateUtil.setLoginGameProfile(loginHandler, profile);
                        LoginStateUtil.setReadyToAccept(loginHandler);
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Nie udało się ustawić profilu/READY_TO_ACCEPT: " + t.getMessage());
                    t.printStackTrace();
                }

                // konsumuj pakiet klienta
                event.setCancelled(true);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    disconnect(event, "Failed to verify username!");
                    event.setCancelled(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    sessions.remove(key);
                }
            }
        }
    }

    private void disconnect(PacketEvent event, String msg) throws Exception {
        PacketContainer dis = pm.createPacket(PacketType.Login.Server.DISCONNECT);
        dis.getChatComponents().write(0, WrappedChatComponent.fromText(msg));
        pm.sendServerPacket(event.getPlayer(), dis);
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
            plugin.getLogger().warning("[tomekf1846-Login] Nie udało się pobrać Connection: " + t.getMessage());
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
}
