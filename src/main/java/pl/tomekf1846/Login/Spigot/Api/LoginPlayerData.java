package pl.tomekf1846.Login.Spigot.Api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LoginPlayerData {
    private final Map<String, String> rawData;
    private final String nick;
    private final String playerUuid;
    private final String premiumUuid;
    private final String firstIp;
    private final String lastIp;
    private final String leaveTime;
    private final String email;
    private final String premium;
    private final String password;
    private final String language;
    private final int version;

    private LoginPlayerData(Map<String, String> rawData) {
        this.rawData = Collections.unmodifiableMap(new HashMap<>(rawData));
        this.nick = rawData.getOrDefault("Nick", "");
        this.playerUuid = rawData.getOrDefault("Player-UUID", "");
        this.premiumUuid = rawData.getOrDefault("Premium-UUID", "");
        this.firstIp = rawData.getOrDefault("FirstIP", "");
        this.lastIp = rawData.getOrDefault("LastIP", "");
        this.leaveTime = rawData.getOrDefault("LeaveTime", "");
        this.email = rawData.getOrDefault("Email", "");
        this.premium = rawData.getOrDefault("Premium", "");
        this.password = rawData.getOrDefault("Password", "");
        this.language = rawData.getOrDefault("Language", "");
        this.version = parseVersion(rawData.get("Version"));
    }

    public static Optional<LoginPlayerData> fromMap(Map<String, String> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LoginPlayerData(rawData));
    }

    public Map<String, String> getRawData() {
        return rawData;
    }

    public String getNick() {
        return nick;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPremiumUuid() {
        return premiumUuid;
    }

    public String getFirstIp() {
        return firstIp;
    }

    public String getLastIp() {
        return lastIp;
    }

    public String getLeaveTime() {
        return leaveTime;
    }

    public String getEmail() {
        return email;
    }

    public String getPremium() {
        return premium;
    }

    public boolean isPremium() {
        return "true".equalsIgnoreCase(premium);
    }

    public String getPassword() {
        return password;
    }

    public String getLanguage() {
        return language;
    }

    public int getVersion() {
        return version;
    }

    private static int parseVersion(String version) {
        if (version == null || version.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
