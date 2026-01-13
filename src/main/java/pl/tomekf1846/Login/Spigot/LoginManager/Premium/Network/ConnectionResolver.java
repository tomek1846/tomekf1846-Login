package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import com.comphenix.protocol.events.PacketEvent;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.crypto.SecretKey;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public class ConnectionResolver {

    private final Plugin plugin;
    private final MinecraftVersionResolver versionResolver;

    public ConnectionResolver(Plugin plugin) {
        this.plugin = plugin;
        this.versionResolver = MinecraftVersionResolver.get();
    }

    public String socketKey(SocketAddress sa) {
        if (sa instanceof InetSocketAddress isa) {
            String host = isa.getAddress() != null ? isa.getAddress().getHostAddress() : isa.getHostString();
            return host + ":" + isa.getPort();
        }
        return sa != null ? sa.toString() : "null";
    }

    public String connKey(PacketEvent event, Object fallbackConnection) {
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

    public Object findConnectionFor(PacketEvent event) {
        try {
            Player p = event.getPlayer();
            if (p == null || p.getAddress() == null) return null;
            InetSocketAddress target = p.getAddress();

            Object craftServer = Bukkit.getServer();
            Method getServer = craftServer.getClass().getMethod("getServer");
            Object mcServer = getServer.invoke(craftServer);
            if (mcServer == null) return null;

            Object scl = resolveServerConnectionListener(mcServer);
            if (scl == null) return null;

            List<Object> connections = new ArrayList<>();
            collectByFqcn(scl, versionResolver.getConnectionClassPrefixes(), 0, new IdentityHashMap<>(), connections);

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

    public Object findConnectionByChannel(Channel channel) {
        if (channel == null) {
            return null;
        }

        try {
            Object craftServer = Bukkit.getServer();
            Method getServer = craftServer.getClass().getMethod("getServer");
            Object mcServer = getServer.invoke(craftServer);
            if (mcServer == null) {
                return null;
            }

            Object scl = resolveServerConnectionListener(mcServer);
            if (scl == null) {
                return null;
            }

            List<Object> connections = new ArrayList<>();
            collectByFqcn(scl, versionResolver.getConnectionClassPrefixes(), 0, new IdentityHashMap<>(), connections);

            for (Object conn : connections) {
                Channel candidate = findChannel(conn);
                if (candidate == channel) {
                    return conn;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Object resolveServerConnectionListener(Object mcServer) {
        if (mcServer == null) {
            return null;
        }

        Object scl = null;
        try {
            Method m = mcServer.getClass().getMethod("getConnection");
            if (m.getParameterCount() == 0 && !m.getReturnType().equals(void.class)) {
                scl = m.invoke(mcServer);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        if (scl == null) {
            try {
                Method legacy = mcServer.getClass().getMethod("getServerConnection");
                if (legacy.getParameterCount() == 0 && !legacy.getReturnType().equals(void.class)) {
                    scl = legacy.invoke(mcServer);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        if (scl == null) {
            scl = extractFieldType(mcServer, versionResolver.getServerConnectionClassPrefixes());
        }
        return scl;
    }

    public Channel resolveChannel(PacketEvent event, Object currentConnection) {
        Channel channel = findChannel(currentConnection);
        if (channel != null) {
            return channel;
        }

        Object resolved = currentConnection;
        if (resolved == null) {
            resolved = findConnectionFor(event);
        }

        return findChannel(resolved);
    }

    public Channel findChannel(Object connection) {
        if (connection == null) {
            return null;
        }

        try {
            try {
                Method channelMethod = connection.getClass().getMethod("channel");
                if (Channel.class.isAssignableFrom(channelMethod.getReturnType())) {
                    channelMethod.setAccessible(true);
                    Object value = channelMethod.invoke(connection);
                    if (value instanceof Channel ch) {
                        return ch;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            }

            for (Class<?> current = connection.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {
                for (var field : current.getDeclaredFields()) {
                    if (!field.getType().getName().equals("io.netty.channel.Channel")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value instanceof Channel ch) {
                        return ch;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static Object extractFieldType(Object obj, String... fqcnStartsWith) {
        if (obj == null || fqcnStartsWith == null || fqcnStartsWith.length == 0) {
            return null;
        }
        for (java.lang.reflect.Field f : obj.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v == null) {
                    continue;
                }
                String className = v.getClass().getName();
                for (String prefix : fqcnStartsWith) {
                    if (prefix != null && !prefix.isEmpty() && className.startsWith(prefix)) {
                        return v;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void collectByFqcn(Object obj,
                                      String[] fqcnPrefixes,
                                      int depth,
                                      IdentityHashMap<Object, Boolean> seen,
                                      List<Object> out) {
        if (obj == null || depth > 8 || seen.containsKey(obj)) return;
        seen.put(obj, Boolean.TRUE);

        Class<?> c = obj.getClass();
        if (matchesAny(c.getName(), fqcnPrefixes)) {
            out.add(obj);
            return;
        }

        if (c.isArray()) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object v = Array.get(obj, i);
                collectByFqcn(v, fqcnPrefixes, depth + 1, seen, out);
            }
            return;
        }

        if (obj instanceof Iterable<?>) {
            for (Object v : (Iterable<?>) obj) {
                collectByFqcn(v, fqcnPrefixes, depth + 1, seen, out);
            }
        }
        if (obj instanceof Map<?, ?>) {
            for (Object v : ((Map<?, ?>) obj).values()) {
                collectByFqcn(v, fqcnPrefixes, depth + 1, seen, out);
            }
        }

        for (java.lang.reflect.Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                collectByFqcn(v, fqcnPrefixes, depth + 1, seen, out);
            } catch (Throwable ignored) {}
        }
    }

    private static boolean matchesAny(String className, String[] prefixes) {
        if (prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void invokeSetupEncryption(Object connection, SecretKey secretKey) throws Exception {
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
}
