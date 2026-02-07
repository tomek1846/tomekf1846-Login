package pl.tomekf1846.Login.Spigot.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerRecord {
    private String nick;
    private String playerUuid;
    private String premiumUuid;
    private String firstIp;
    private String lastIp;
    private String leaveTime;
    private String email;
    private String premium;
    private String password;
    private String language;
    private int version;
    private List<String> playerIP;
    private List<LoginAttemptRecord> loginAttempts;

    public static PlayerRecord fromDefaults(UUID uuid, String nick, String firstIp, String password, String language) {
        PlayerRecord record = new PlayerRecord();
        record.nick = nick;
        record.playerUuid = uuid.toString();
        record.premiumUuid = "";
        record.firstIp = firstIp;
        record.lastIp = "";
        record.leaveTime = "";
        record.email = "none";
        record.premium = "";
        record.password = password;
        record.language = language;
        record.version = 1;
        record.playerIP = new ArrayList<>();
        return record;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Nick", nick);
        map.put("Player-UUID", playerUuid);
        map.put("Premium-UUID", premiumUuid);
        map.put("FirstIP", firstIp);
        map.put("LastIP", lastIp);
        map.put("LeaveTime", leaveTime);
        map.put("Email", email);
        map.put("Premium", premium);
        map.put("Password", password);
        map.put("Language", language);
        map.put("Version", String.valueOf(version));
        return map;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPremiumUuid() {
        return premiumUuid;
    }

    public void setPremiumUuid(String premiumUuid) {
        this.premiumUuid = premiumUuid;
    }

    public String getFirstIp() {
        return firstIp;
    }

    public void setFirstIp(String firstIp) {
        this.firstIp = firstIp;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public String getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(String leaveTime) {
        this.leaveTime = leaveTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPremium() {
        return premium;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getPlayerIP() {
        if (playerIP == null) {
            playerIP = new ArrayList<>();
        }
        return playerIP;
    }

    public void setPlayerIP(List<String> playerIP) {
        this.playerIP = playerIP;
    }

    public List<LoginAttemptRecord> getLoginAttempts() {
        if (loginAttempts == null) {
            loginAttempts = new ArrayList<>();
        }
        return loginAttempts;
    }

    public void setLoginAttempts(List<LoginAttemptRecord> loginAttempts) {
        this.loginAttempts = loginAttempts;
    }
}
