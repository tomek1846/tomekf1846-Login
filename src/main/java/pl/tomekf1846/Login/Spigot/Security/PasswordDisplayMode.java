package pl.tomekf1846.Login.Spigot.Security;

public enum PasswordDisplayMode {
    PLAIN,
    MASKED,
    HIDDEN;

    public static PasswordDisplayMode fromString(String value) {
        if (value == null) {
            return MASKED;
        }
        try {
            return PasswordDisplayMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MASKED;
        }
    }
}
