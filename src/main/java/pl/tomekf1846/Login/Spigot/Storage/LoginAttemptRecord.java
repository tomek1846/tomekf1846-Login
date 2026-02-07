package pl.tomekf1846.Login.Spigot.Storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LoginAttemptRecord {
    private final UUID uuid;
    private final String timestamp;
    private final boolean success;
    private final String attemptedPassword;
    private final int wrongAttempts;
    private final String ipAddress;
    private final String snapshotJson;

    public LoginAttemptRecord(UUID uuid,
                              String timestamp,
                              boolean success,
                              String attemptedPassword,
                              int wrongAttempts,
                              String ipAddress,
                              String snapshotJson) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.success = success;
        this.attemptedPassword = attemptedPassword;
        this.wrongAttempts = wrongAttempts;
        this.ipAddress = ipAddress;
        this.snapshotJson = snapshotJson;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getAttemptedPassword() {
        return attemptedPassword;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Timestamp", timestamp);
        data.put("Success", success);
        data.put("AttemptedPassword", attemptedPassword);
        data.put("WrongAttempts", wrongAttempts);
        data.put("IpAddress", ipAddress);
        data.put("Snapshot", snapshotJson);
        return data;
    }
}
