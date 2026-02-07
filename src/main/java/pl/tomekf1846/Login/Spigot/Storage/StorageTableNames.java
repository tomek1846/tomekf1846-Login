package pl.tomekf1846.Login.Spigot.Storage;

public class StorageTableNames {
    private final String playerTable;
    private final String securityTable;
    private final String ipHistoryTable;
    private final String loginAttemptsTable;
    private final String passwordHistoryTable;

    public StorageTableNames(String playerTable, String securityTable, String ipHistoryTable, String loginAttemptsTable, String passwordHistoryTable) {
        this.playerTable = playerTable;
        this.securityTable = securityTable;
        this.ipHistoryTable = ipHistoryTable;
        this.loginAttemptsTable = loginAttemptsTable;
        this.passwordHistoryTable = passwordHistoryTable;
    }

    public String getPlayerTable() {
        return playerTable;
    }

    public String getSecurityTable() {
        return securityTable;
    }

    public String getIpHistoryTable() {
        return ipHistoryTable;
    }

    public String getLoginAttemptsTable() {
        return loginAttemptsTable;
    }

    public String getPasswordHistoryTable() {
        return passwordHistoryTable;
    }
}
