package pl.tomekf1846.Login.Spigot.Storage;

public class StorageTableNames {
    private final String playerTable;
    private final String securityTable;
    private final String ipHistoryTable;

    public StorageTableNames(String playerTable, String securityTable, String ipHistoryTable) {
        this.playerTable = playerTable;
        this.securityTable = securityTable;
        this.ipHistoryTable = ipHistoryTable;
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
}
