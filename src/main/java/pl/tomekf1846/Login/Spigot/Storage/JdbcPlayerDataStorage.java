package pl.tomekf1846.Login.Spigot.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private final String tableName;
    private final SqlDialect dialect;
    private final Gson gson;

    public JdbcPlayerDataStorage(JavaPlugin plugin, HikariDataSource dataSource, String tableName, SqlDialect dialect) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.dialect = dialect;
        this.gson = new GsonBuilder().create();
        ensureTable();
    }

    private void ensureTable() {
        String quotedTable = dialect.quote(tableName);
        String sql = "CREATE TABLE IF NOT EXISTS " + quotedTable + " ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "nick VARCHAR(32),"
                + "premium_uuid VARCHAR(36),"
                + "first_ip VARCHAR(45),"
                + "last_ip VARCHAR(45),"
                + "leave_time VARCHAR(32),"
                + "email VARCHAR(100),"
                + "premium VARCHAR(5),"
                + "password VARCHAR(255),"
                + "version INT,"
                + "ip_history TEXT"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create player data table.", ex);
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
        upsertRecord(uuid, record);
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
        upsertRecord(uuid, record);
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
        String sql = "SELECT uuid, nick, premium_uuid, first_ip, last_ip, leave_time, email, premium, password, version, ip_history "
                + "FROM " + dialect.quote(tableName);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                PlayerRecord record = recordFromResultSet(resultSet);
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
        String sql = "UPDATE " + dialect.quote(tableName) + " SET leave_time=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, leaveTime);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed saving leave time for " + uuid, ex);
        }
    }

    @Override
    public void setPlayerPassword(UUID uuid, String newPassword) {
        if (uuid == null || newPassword == null || newPassword.isEmpty()) {
            return;
        }
        String sql = "UPDATE " + dialect.quote(tableName) + " SET password=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating password for " + uuid, ex);
        }
    }

    @Override
    public void setPlayerEmail(UUID uuid, String newEmail) {
        if (uuid == null || newEmail == null || newEmail.isEmpty()) {
            return;
        }
        String sql = "UPDATE " + dialect.quote(tableName) + " SET email=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newEmail);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
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
        String sql = "UPDATE " + dialect.quote(tableName) + " SET premium=? WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(isPremium));
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
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
        String sql = "SELECT uuid FROM " + dialect.quote(tableName) + " WHERE LOWER(nick)=LOWER(?) LIMIT 1";
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
        String sql = "DELETE FROM " + dialect.quote(tableName) + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            return statement.executeUpdate() > 0;
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
        String sql = "SELECT uuid, nick, premium_uuid, first_ip, last_ip, leave_time, email, premium, password, version, ip_history "
                + "FROM " + dialect.quote(tableName) + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return recordFromResultSet(resultSet);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed reading player data for " + uuid, ex);
        }
        return null;
    }

    private void upsertRecord(UUID uuid, PlayerRecord record) {
        if (record == null) {
            return;
        }
        if (exists(uuid)) {
            updateRecord(uuid, record);
        } else {
            insertRecord(uuid, record);
        }
    }

    private boolean exists(UUID uuid) {
        String sql = "SELECT 1 FROM " + dialect.quote(tableName) + " WHERE uuid=?";
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

    private void insertRecord(UUID uuid, PlayerRecord record) {
        String sql = "INSERT INTO " + dialect.quote(tableName)
                + " (uuid, nick, premium_uuid, first_ip, last_ip, leave_time, email, premium, password, version, ip_history)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, uuid, record);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed inserting player data for " + uuid, ex);
        }
    }

    private void updateRecord(UUID uuid, PlayerRecord record) {
        String sql = "UPDATE " + dialect.quote(tableName)
                + " SET nick=?, premium_uuid=?, first_ip=?, last_ip=?, leave_time=?, email=?, premium=?, password=?, version=?, ip_history=?"
                + " WHERE uuid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.getNick());
            statement.setString(2, record.getPremiumUuid());
            statement.setString(3, record.getFirstIp());
            statement.setString(4, record.getLastIp());
            statement.setString(5, record.getLeaveTime());
            statement.setString(6, record.getEmail());
            statement.setString(7, record.getPremium());
            statement.setString(8, record.getPassword());
            statement.setInt(9, record.getVersion());
            statement.setString(10, gson.toJson(record.getPlayerIP()));
            statement.setString(11, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed updating player data for " + uuid, ex);
        }
    }

    private void fillStatement(PreparedStatement statement, UUID uuid, PlayerRecord record) throws SQLException {
        statement.setString(1, uuid.toString());
        statement.setString(2, record.getNick());
        statement.setString(3, record.getPremiumUuid());
        statement.setString(4, record.getFirstIp());
        statement.setString(5, record.getLastIp());
        statement.setString(6, record.getLeaveTime());
        statement.setString(7, record.getEmail());
        statement.setString(8, record.getPremium());
        statement.setString(9, record.getPassword());
        statement.setInt(10, record.getVersion());
        statement.setString(11, gson.toJson(record.getPlayerIP()));
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
        String ipHistory = resultSet.getString("ip_history");
        if (ipHistory != null && !ipHistory.isEmpty()) {
            List<String> history = gson.fromJson(ipHistory, List.class);
            record.setPlayerIP(history);
        } else {
            record.setPlayerIP(new ArrayList<>());
        }
        return record;
    }
}
