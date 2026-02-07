package pl.tomekf1846.Login.Spigot.Storage;

import java.util.Locale;

public enum StorageType {
    YML,
    JSON,
    H2,
    MYSQL,
    MARIADB,
    POSTGRESQL;

    public static StorageType fromString(String value) {
        if (value == null) {
            return YML;
        }
        try {
            return StorageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return YML;
        }
    }
}
