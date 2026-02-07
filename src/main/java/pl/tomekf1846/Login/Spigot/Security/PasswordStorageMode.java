package pl.tomekf1846.Login.Spigot.Security;

public enum PasswordStorageMode {
    NONE,
    AES_GCM,
    AES_CBC,
    CHACHA20;

    public static PasswordStorageMode fromString(String value) {
        if (value == null) {
            return AES_GCM;
        }
        try {
            return PasswordStorageMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AES_GCM;
        }
    }
}
