package pl.tomekf1846.Login.Spigot.Storage;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;

public class JdbcPlayerDataStorage implements PlayerDataStorage {
    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final String playerTable;
    private final String securityTable;
    private final String historyTable;
    private final String loginAttemptsTable;
    private final SqlDialect dialect;
    public JdbcPlayerDataStorage(JavaPlugin plugin, HikariDataSource dataSource, StorageTableNames tableNames, SqlDialect dialect) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.playerTable = tableNames.getPlayerTable();
        this.securityTable = tableNames.getSecurityTable();
        this.historyTable = tableNames.getIpHistoryTable();
        this.loginAttemptsTable = tableNames.getLoginAttemptsTable();
        this.dialect = dialect;
        ensureTables();
    }

    private void ensureTables() {
        String quotedPlayers = dialect.quote(playerTable);
        String quotedSecurity = dialect.quote(securityTable);
        String quotedHistory = dialect.quote(historyTable);
        String quotedLoginAttempts = dialect.quote(loginAttemptsTable);
        String playerSql = "CREATE TABLE IF NOT EXISTS " + quotedPlayers + " ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "nick VARCHAR(32),"
                + "premium_uuid VARCHAR(36),"
                + "email VARCHAR(100),"
                + "premium VARCHAR(5),"
                + "version INT,"
                + "created_at VARCHAR(32),"
                + "updated_at VARCHAR(32)"
                + ")";
        String securitySql = "CREATE TABLE IF NOT EXISTS " + quotedSecurity + " ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "password VARCHAR(255),"
                + "first_ip VARCHAR(45),"
                + "last_ip VARCHAR(45),"
                + "leave_time VARCHAR(32),"
                + "last_login VARCHAR(32)"
                + ")";
        String historySql = "CREATE TABLE IF NOT EXISTS " + quotedHistory + " ("
                + "uuid VARCHAR(36),"
                + "ip_entry TEXT,"
                + "created_at VARCHAR(32)"
                + ")";
        String loginSql = "CREATE TABLE IF NOT EXISTS " + quotedLoginAttempts + " ("
                + "entry_id VARCHAR(36) PRIMARY KEY,"
                + "uuid VARCHAR(36),"
                + "created_at VARCHAR(32),"
                + "success VARCHAR(5),"
                + "attempted_password VARCHAR(255),"
                + "wrong_attempts INT,"
                + "ip_address VARCHAR(45),"
                + "snapshot_json TEXT"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(playerSql);
            statement.execute(securitySql);
            statement.execute(historySql);
            statement.execute(loginSql);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create player data tables.", ex);
        }
    }

    @Override
    public void savePlayerData(OfflinePlayer player, String password) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String firstIP = (player.isOnline() && player.getPlayer() != null)
                ? player.getPlayer().getAddress().getAddress().getHostAddress()
                : "offline";
        PlayerRecord record = PlayerRecord.fromDefaults(uuid, playerName, firstIP, password);
        upsertRecord(uuid, record, nowTimestamp());
    }

    @Override
    public void savePlayerIPHistory(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            record = PlayerRecord.fromDefaults(uuid, player.getName(), "offline", "none");
        }
        String currentIP = player.getAddress().getAddress().getHostAddress();
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        List<String> history = record.getPlayerIP();
        history.add(timestamp + " - " + currentIP);
        if (record.getFirstIp() == null || record.getFirstIp().isEmpty()) {
            record.setFirstIp(currentIP);
        }
        record.setLastIp(currentIP);
        upsertRecord(uuid, record, nowTimestamp());
        updateSecurityLogin(uuid, record, timestamp);
        insertIpHistory(uuid, timestamp + " - " + currentIP, timestamp);
    }

    @Override
    public Map<String, String> loadPlayerData(UUID uuid) {
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return null;
        }
        return record.toMap();
    }

    @Override
    public Map<UUID, Map<String, String>> loadAllPlayerData() {
        Map<UUID, Map<String, String>> allPlayerData = new HashMap<>();
        String sql = "SELECT p.uuid, p.nick, p.premium_uuid, s.first_ip, s.last_ip, s.leave_time, p.email, p.premium, s.password, p.version "
                + "FROM " + dialect.quote(playerTable) + " p "
                + "LEFT JOIN " + dialect.quote(securityTable) + " s ON p.uuid = s.uuid";
        Map<UUID, List<String>> historyMap = loadAllIpHistory();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                PlayerRecord record = recordFromResultSet(resultSet);
                record.setPlayerIP(historyMap.getOrDefault(uuid, new ArrayList<>()));
                allPlayerData.put(uuid, record.toMap());
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed loading all player data.", ex);
        }
        return allPlayerData;
    }

    @Override
    public void savePlayerLeaveTime(Player player) {
        UUID uuid = player.getUniqueId();
        String leaveTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        String sql = "UPDATE " + dialect.quote(securityTable) + " SET leave_time=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, leaveTime);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
            touchUpdatedAt(uuid);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed saving leave time for " + uuid, ex);
        }
    }

    @Override
    public void saveLoginAttempt(LoginAttemptRecord attempt) {
        if (attempt == null) {
            return;
        }
        String sql = "INSERT INTO " + dialect.quote(loginAttemptsTable)
                + " (entry_id, uuid, created_at, success, attempted_password, wrong_attempts, ip_address, snapshot_json)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, attempt.getUuid().toString());
            statement.setString(3, attempt.getTimestamp());
            statement.setString(4, String.valueOf(attempt.isSuccess()));
            statement.setString(5, attempt.getAttemptedPassword());
            statement.setInt(6, attempt.getWrongAttempts());
            statement.setString(7, attempt.getIpAddress());
            statement.setString(8, attempt.getSnapshotJson());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed saving login attempt for " + attempt.getUuid(), ex);
        }
    }

    @Override
    public void setPlayerPassword(UUID uuid, String newPassword) {
        if (uuid == null || newPassword == null || newPassword.isEmpty()) {
            return;
        }
        String sql = "UPDATE " + dialect.quote(securityTable) + " SET password=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
            touchUpdatedAt(uuid);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating password for " + uuid, ex);
        }
    }

    @Override
    public void setPlayerEmail(UUID uuid, String newEmail) {
        if (uuid == null || newEmail == null || newEmail.isEmpty()) {
            return;
        }
        String sql = "UPDATE " + dialect.quote(playerTable) + " SET email=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newEmail);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
            touchUpdatedAt(uuid);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating email for " + uuid, ex);
        }
    }

    @Override
    public boolean setPlayerSession(String nick, boolean isPremium) {
        UUID uuid = findUUIDByNick(nick);
        if (uuid == null) {
            return false;
        }
        String sql = "UPDATE " + dialect.quote(playerTable) + " SET premium=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(isPremium));
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
            touchUpdatedAt(uuid);
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating premium session for " + uuid, ex);
            return false;
        }
    }

    @Override
    public UUID findUUIDByNick(String nick) {
        if (nick == null || nick.isEmpty()) {
            return null;
        }
        String sql = "SELECT uuid FROM " + dialect.quote(playerTable) + " WHERE LOWER(nick)=LOWER(?) LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nick);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return UUID.fromString(resultSet.getString("uuid"));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed finding UUID for nick " + nick, ex);
        }
        return null;
    }

    @Override
    public boolean deletePlayerData(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        String deleteHistory = "DELETE FROM " + dialect.quote(historyTable) + " WHERE uuid=?";
        String deleteSecurity = "DELETE FROM " + dialect.quote(securityTable) + " WHERE uuid=?";
        String deletePlayer = "DELETE FROM " + dialect.quote(playerTable) + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement historyStatement = connection.prepareStatement(deleteHistory);
             PreparedStatement securityStatement = connection.prepareStatement(deleteSecurity);
             PreparedStatement playerStatement = connection.prepareStatement(deletePlayer)) {
            historyStatement.setString(1, uuid.toString());
            historyStatement.executeUpdate();
            securityStatement.setString(1, uuid.toString());
            securityStatement.executeUpdate();
            playerStatement.setString(1, uuid.toString());
            return playerStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed deleting player data for " + uuid, ex);
            return false;
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private PlayerRecord readRecord(UUID uuid) {
        String sql = "SELECT p.uuid, p.nick, p.premium_uuid, s.first_ip, s.last_ip, s.leave_time, "
                + "p.email, p.premium, s.password, p.version "
                + "FROM " + dialect.quote(playerTable) + " p "
                + "LEFT JOIN " + dialect.quote(securityTable) + " s ON p.uuid = s.uuid "
                + "WHERE p.uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerRecord record = recordFromResultSet(resultSet);
                    record.setPlayerIP(loadIpHistory(uuid));
                    return record;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed reading player data for " + uuid, ex);
        }
        return null;
    }

    private void upsertRecord(UUID uuid, PlayerRecord record, String timestamp) {
        if (record == null) {
            return;
        }
        if (exists(uuid)) {
            updateRecord(uuid, record, timestamp);
        } else {
            insertRecord(uuid, record, timestamp);
        }
    }

    private boolean exists(UUID uuid) {
        String sql = "SELECT 1 FROM " + dialect.quote(playerTable) + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed checking player existence for " + uuid, ex);
            return false;
        }
    }

    private void insertRecord(UUID uuid, PlayerRecord record, String timestamp) {
        String playerSql = "INSERT INTO " + dialect.quote(playerTable)
                + " (uuid, nick, premium_uuid, email, premium, version, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String securitySql = "INSERT INTO " + dialect.quote(securityTable)
                + " (uuid, password, first_ip, last_ip, leave_time, last_login)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement playerStatement = connection.prepareStatement(playerSql);
             PreparedStatement securityStatement = connection.prepareStatement(securitySql)) {
            fillPlayerStatement(playerStatement, uuid, record, timestamp);
            playerStatement.executeUpdate();
            fillSecurityStatement(securityStatement, uuid, record, null);
            securityStatement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed inserting player data for " + uuid, ex);
        }
    }

    private void updateRecord(UUID uuid, PlayerRecord record, String timestamp) {
        String playerSql = "UPDATE " + dialect.quote(playerTable)
                + " SET nick=?, premium_uuid=?, email=?, premium=?, version=?, updated_at=?"
                + " WHERE uuid=?";
        String securitySql = "UPDATE " + dialect.quote(securityTable)
                + " SET password=?, first_ip=?, last_ip=?, leave_time=?"
                + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement playerStatement = connection.prepareStatement(playerSql);
             PreparedStatement securityStatement = connection.prepareStatement(securitySql)) {
            playerStatement.setString(1, record.getNick());
            playerStatement.setString(2, record.getPremiumUuid());
            playerStatement.setString(3, record.getEmail());
            playerStatement.setString(4, record.getPremium());
            playerStatement.setInt(5, record.getVersion());
            playerStatement.setString(6, timestamp);
            playerStatement.setString(7, uuid.toString());
            playerStatement.executeUpdate();

            securityStatement.setString(1, record.getPassword());
            securityStatement.setString(2, record.getFirstIp());
            securityStatement.setString(3, record.getLastIp());
            securityStatement.setString(4, record.getLeaveTime());
            securityStatement.setString(5, uuid.toString());
            securityStatement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating player data for " + uuid, ex);
        }
    }

    private void fillPlayerStatement(PreparedStatement statement, UUID uuid, PlayerRecord record, String timestamp) throws SQLException {
        statement.setString(1, uuid.toString());
        statement.setString(2, record.getNick());
        statement.setString(3, record.getPremiumUuid());
        statement.setString(4, record.getEmail());
        statement.setString(5, record.getPremium());
        statement.setInt(6, record.getVersion());
        statement.setString(7, timestamp);
        statement.setString(8, timestamp);
    }

    private void fillSecurityStatement(PreparedStatement statement, UUID uuid, PlayerRecord record, String lastLogin) throws SQLException {
        statement.setString(1, uuid.toString());
        statement.setString(2, record.getPassword());
        statement.setString(3, record.getFirstIp());
        statement.setString(4, record.getLastIp());
        statement.setString(5, record.getLeaveTime());
        statement.setString(6, lastLogin);
    }

    private PlayerRecord recordFromResultSet(ResultSet resultSet) throws SQLException {
        PlayerRecord record = new PlayerRecord();
        record.setPlayerUuid(resultSet.getString("uuid"));
        record.setNick(resultSet.getString("nick"));
        record.setPremiumUuid(resultSet.getString("premium_uuid"));
        record.setFirstIp(resultSet.getString("first_ip"));
        record.setLastIp(resultSet.getString("last_ip"));
        record.setLeaveTime(resultSet.getString("leave_time"));
        record.setEmail(resultSet.getString("email"));
        record.setPremium(resultSet.getString("premium"));
        record.setPassword(resultSet.getString("password"));
        record.setVersion(resultSet.getInt("version"));
        return record;
    }

    private void updateSecurityLogin(UUID uuid, PlayerRecord record, String loginTimestamp) {
        String sql = "UPDATE " + dialect.quote(securityTable)
                + " SET first_ip=?, last_ip=?, last_login=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.getFirstIp());
            statement.setString(2, record.getLastIp());
            statement.setString(3, loginTimestamp);
            statement.setString(4, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating login metadata for " + uuid, ex);
        }
    }

    private void insertIpHistory(UUID uuid, String entry, String timestamp) {
        String sql = "INSERT INTO " + dialect.quote(historyTable) + " (uuid, ip_entry, created_at) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, entry);
            statement.setString(3, timestamp);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed inserting IP history for " + uuid, ex);
        }
    }

    private List<String> loadIpHistory(UUID uuid) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT ip_entry FROM " + dialect.quote(historyTable) + " WHERE uuid=? ORDER BY created_at";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(resultSet.getString("ip_entry"));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed loading IP history for " + uuid, ex);
        }
        return history;
    }

    private Map<UUID, List<String>> loadAllIpHistory() {
        Map<UUID, List<String>> history = new HashMap<>();
        String sql = "SELECT uuid, ip_entry, created_at FROM " + dialect.quote(historyTable) + " ORDER BY created_at";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                history.computeIfAbsent(uuid, key -> new ArrayList<>()).add(resultSet.getString("ip_entry"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed loading IP history entries.", ex);
        }
        return history;
    }

    private void touchUpdatedAt(UUID uuid) {
        String sql = "UPDATE " + dialect.quote(playerTable) + " SET updated_at=? WHERE uuid=?";
        String timestamp = nowTimestamp();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, timestamp);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating updated_at for " + uuid, ex);
        }
    }

    private String nowTimestamp() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
    }
}
