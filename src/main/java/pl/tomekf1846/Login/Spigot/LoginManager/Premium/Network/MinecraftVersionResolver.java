package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class MinecraftVersionResolver {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static final MinecraftVersionResolver INSTANCE = new MinecraftVersionResolver();

    private final String bukkitVersion;
    private final String craftBukkitPackage;
    private final String nmsVersionToken;
    private final boolean mojangMapped;
    private final int major;
    private final int minor;
    private final int patch;
    private final String legacyBasePackage;

    private final String[] connectionClassPrefixes;
    private final String[] loginHandlerClassPrefixes;
    private final String[] serverConnectionClassPrefixes;
    private final String[] chatComponentClasses;
    private final String[] chatComponentSerializerClasses;

    private MinecraftVersionResolver() {
        this.bukkitVersion = Bukkit.getBukkitVersion();
        this.craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        this.nmsVersionToken = craftBukkitPackage.substring(craftBukkitPackage.lastIndexOf('.') + 1);

        boolean mojang = !classExists("net.minecraft.server." + nmsVersionToken + ".MinecraftServer");
        this.mojangMapped = mojang;

        int[] parsed = parseVersionNumbers(bukkitVersion);
        this.major = parsed[0];
        this.minor = parsed[1];
        this.patch = parsed[2];

        if (mojang) {
            this.legacyBasePackage = null;
            this.connectionClassPrefixes = new String[] {
                    "net.minecraft.network.Connection",
                    "net.minecraft.network.NetworkManager"
            };
            this.loginHandlerClassPrefixes = new String[] {
                    "net.minecraft.server.network.ServerLoginPacketListenerImpl",
                    "net.minecraft.server.network.LoginListener",
                    "net.minecraft.server.network.ServerLoginNetHandler"
            };
            this.serverConnectionClassPrefixes = new String[] {
                    "net.minecraft.server.network.ServerConnectionListener",
                    "net.minecraft.server.network.ServerConnection",
                    "net.minecraft.server.network.ServerConnectionListener$",
                    "net.minecraft.server.network.ServerConnection$"
            };
            this.chatComponentClasses = new String[] {
                    "net.minecraft.network.chat.Component"
            };
            this.chatComponentSerializerClasses = new String[] {
                    "net.minecraft.network.chat.Component$Serializer",
                    "net.minecraft.network.chat.Component$ChatSerializer",
                    "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer"
            };
        } else {
            String base = "net.minecraft.server." + nmsVersionToken;
            this.legacyBasePackage = base;
            this.connectionClassPrefixes = new String[] {
                    base + ".NetworkManager",
                    base + ".PendingConnection"
            };
            this.loginHandlerClassPrefixes = new String[] {
                    base + ".LoginListener",
                    base + ".PendingConnection",
                    base + ".LoginListener$1",
                    base + ".ServerLoginPacketListenerImpl"
            };
            this.serverConnectionClassPrefixes = new String[] {
                    base + ".ServerConnection",
                    base + ".ServerConnectionListener",
                    base + ".DedicatedServerConnection",
                    base + ".ServerConnection$"
            };
            this.chatComponentClasses = new String[] {
                    base + ".IChatBaseComponent",
                    base + ".ChatComponentText"
            };
            this.chatComponentSerializerClasses = new String[] {
                    base + ".IChatBaseComponent$ChatSerializer",
                    base + ".ChatSerializer"
            };
        }
    }

    public static MinecraftVersionResolver get() {
        return INSTANCE;
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isMojangMapped() {
        return mojangMapped;
    }

    public String getNmsVersionToken() {
        return nmsVersionToken;
    }

    public String getLegacyBasePackage() {
        return legacyBasePackage;
    }

    public String[] getConnectionClassPrefixes() {
        return Arrays.copyOf(connectionClassPrefixes, connectionClassPrefixes.length);
    }

    public String[] getLoginHandlerClassPrefixes() {
        return Arrays.copyOf(loginHandlerClassPrefixes, loginHandlerClassPrefixes.length);
    }

    public String[] getServerConnectionClassPrefixes() {
        return Arrays.copyOf(serverConnectionClassPrefixes, serverConnectionClassPrefixes.length);
    }

    public String[] getChatComponentClasses() {
        return Arrays.copyOf(chatComponentClasses, chatComponentClasses.length);
    }

    public String[] getChatComponentSerializerClasses() {
        return Arrays.copyOf(chatComponentSerializerClasses, chatComponentSerializerClasses.length);
    }

    public boolean isAtLeast(int major, int minor) {
        if (this.major > major) {
            return true;
        }
        if (this.major == major) {
            return this.minor >= minor;
        }
        return false;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        if (this.major > major) {
            return true;
        }
        if (this.major < major) {
            return false;
        }
        if (this.minor > minor) {
            return true;
        }
        if (this.minor < minor) {
            return false;
        }
        return this.patch >= patch;
    }

    private static int[] parseVersionNumbers(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(Objects.toString(version, ""));
        if (!matcher.find()) {
            return new int[] {0, 0, 0};
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        String patchGroup = matcher.group(3);
        int patch = patchGroup != null ? Integer.parseInt(patchGroup) : 0;
        return new int[] {major, minor, patch};
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
