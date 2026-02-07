package pl.tomekf1846.Login.Spigot.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class StorageFactory {
    private StorageFactory() {
    }

    public static PlayerDataStorage create(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        StorageType type = StorageType.fromString(config.getString("Storage.Type", "YML"));
        switch (type) {
            case JSON:
                return new JsonPlayerDataStorage(plugin, new File(plugin.getDataFolder(), "JSON"));
            case H2:
                return createH2Storage(plugin, config);
            case MYSQL:
                return createRemoteStorage(plugin, config, StorageType.MYSQL);
            case MARIADB:
                return createRemoteStorage(plugin, config, StorageType.MARIADB);
            case POSTGRESQL:
                return createRemoteStorage(plugin, config, StorageType.POSTGRESQL);
            case YML:
            default:
                return new YamlPlayerDataStorage(plugin, new File(plugin.getDataFolder(), "YML"));
        }
    }

    private static PlayerDataStorage createH2Storage(JavaPlugin plugin, FileConfiguration config) {
        String fileName = config.getString("Storage.H2.File-Name", "tomekf1846-login");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        String jdbcUrl = "jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
        StorageTableNames tableNames = resolveTableNames(config, "Storage.H2", "tomekf1846-login-users");
        HikariConfig hikariConfig = baseHikariConfig(config);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getString("Storage.H2.Username", "sa"));
        hikariConfig.setPassword(config.getString("Storage.H2.Password", ""));
        hikariConfig.setDriverClassName("org.h2.Driver");
        return new JdbcPlayerDataStorage(plugin, new HikariDataSource(hikariConfig), tableNames, SqlDialect.H2);
    }

    private static PlayerDataStorage createRemoteStorage(JavaPlugin plugin, FileConfiguration config, StorageType type) {
        StorageTableNames tableNames = resolveTableNames(config, "Storage.Remote", "tomekf1846-login-users");
        String jdbcUrl = config.getString("Storage.Remote.Jdbc-Url", "").trim();
        if (jdbcUrl.isEmpty()) {
            String host = config.getString("Storage.Remote.Host", "localhost");
            int port = config.getInt("Storage.Remote.Port", defaultPort(type));
            String database = config.getString("Storage.Remote.Database", "minecraft");
            boolean useSSL = config.getBoolean("Storage.Remote.Use-SSL", false);
            String params = config.getString("Storage.Remote.Additional-Parameters", "");
            jdbcUrl = buildJdbcUrl(type, host, port, database, useSSL, params);
        }

        HikariConfig hikariConfig = baseHikariConfig(config);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getString("Storage.Remote.Username", "root"));
        hikariConfig.setPassword(config.getString("Storage.Remote.Password", ""));
        hikariConfig.setDriverClassName(driverFor(type));

        return new JdbcPlayerDataStorage(plugin, new HikariDataSource(hikariConfig), tableNames, dialectFor(type));
    }

    private static HikariConfig baseHikariConfig(FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(config.getInt("Storage.Hikari.Maximum-Pool-Size", 10));
        hikariConfig.setMinimumIdle(config.getInt("Storage.Hikari.Minimum-Idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("Storage.Hikari.Connection-Timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("Storage.Hikari.Idle-Timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("Storage.Hikari.Max-Lifetime", 1800000));
        hikariConfig.setPoolName("tomekf1846-login");
        return hikariConfig;
    }

    private static StorageTableNames resolveTableNames(FileConfiguration config, String prefix, String defaultLegacyName) {
        String legacyName = config.getString(prefix + ".Table-Name", defaultLegacyName);
        String playerTable = config.getString(prefix + ".Tables.Players", legacyName);
        String securityTable = config.getString(prefix + ".Tables.Security", legacyName + "_security");
        String ipHistoryTable = config.getString(prefix + ".Tables.Ip-History", legacyName + "_ip_history");
        String loginAttemptsTable = config.getString(prefix + ".Tables.Login-Attempts", legacyName + "_login_attempts");
        String passwordHistoryTable = config.getString(prefix + ".Tables.Password-History", legacyName + "_password_history");
        return new StorageTableNames(playerTable, securityTable, ipHistoryTable, loginAttemptsTable, passwordHistoryTable);
    }

    private static String buildJdbcUrl(StorageType type, String host, int port, String database, boolean useSSL, String extra) {
        String params = extra == null ? "" : extra.trim();
        if (!params.isEmpty() && !params.startsWith("?")) {
            params = "?" + params;
        }
        switch (type) {
            case MARIADB: {
                String sslPart = "useSSL=" + (useSSL ? "true" : "false");
                return "jdbc:mariadb://" + host + ":" + port + "/" + database
                        + (params.isEmpty() ? "?" + sslPart : params + "&" + sslPart);
            }
            case POSTGRESQL:
                return "jdbc:postgresql://" + host + ":" + port + "/" + database + params;
            case MYSQL:
            default: {
                String sslPart = "useSSL=" + (useSSL ? "true" : "false");
                return "jdbc:mysql://" + host + ":" + port + "/" + database
                        + (params.isEmpty() ? "?" + sslPart : params + "&" + sslPart);
            }
        }
    }

    private static int defaultPort(StorageType type) {
        switch (type) {
            case POSTGRESQL:
                return 5432;
            case MARIADB:
                return 3306;
            case MYSQL:
            default:
                return 3306;
        }
    }

    private static String driverFor(StorageType type) {
        switch (type) {
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case MARIADB:
                return "org.mariadb.jdbc.Driver";
            case MYSQL:
            default:
                return "com.mysql.cj.jdbc.Driver";
        }
    }

    private static SqlDialect dialectFor(StorageType type) {
        switch (type) {
            case POSTGRESQL:
                return SqlDialect.POSTGRESQL;
            case MARIADB:
                return SqlDialect.MARIADB;
            case MYSQL:
            default:
                return SqlDialect.MYSQL;
        }
    }
}
