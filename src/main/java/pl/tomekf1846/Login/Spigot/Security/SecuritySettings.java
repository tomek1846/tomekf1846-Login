package pl.tomekf1846.Login.Spigot.Security;

import org.bukkit.configuration.file.FileConfiguration;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecuritySettings {
    private SecuritySettings() {
    }

    private static FileConfiguration config() {
        return MainSpigot.getInstance().getConfig();
    }

    public static PasswordStorageMode getPasswordStorageMode() {
        return PasswordStorageMode.fromString(config().getString("Security.Password-Storage.Mode", "AES_GCM"));
    }

    public static String getPasswordSecret() {
        return config().getString("Security.Password-Storage.Secret", "");
    }

    public static boolean isPasswordHistoryEnabled() {
        return config().getBoolean("Security.Password-History.Enabled", true);
    }

    public static PasswordDisplayMode getPasswordDisplayMode() {
        return PasswordDisplayMode.fromString(config().getString("Security.Gui.Password-Display", "MASKED"));
    }

    public static String getHiddenPasswordText() {
        return config().getString("Security.Gui.Hidden-Text", "hidden");
    }

    public static boolean isCommandLogHiddenEnabled() {
        return config().getBoolean("Security.Console.Hide-Command-Input", true);
    }

    public static List<String> getHiddenCommands() {
        List<String> raw = config().getStringList("Security.Console.Hidden-Commands");
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String entry : raw) {
            if (entry != null && !entry.isBlank()) {
                normalized.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }
}
