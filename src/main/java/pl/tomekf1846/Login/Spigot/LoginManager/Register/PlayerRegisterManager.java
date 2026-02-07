package pl.tomekf1846.Login.Spigot.LoginManager.Register;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.io.File;
import java.util.List;

public class PlayerRegisterManager {
    public static void registerPlayer(Player player, String password, String confirmPassword) {
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        if (isPlayerRegistered(player)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.already_registered"));
            return;
        }

        if (!password.equals(confirmPassword)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.passwords-do-not-match"));
            return;
        }

        FileConfiguration config = MainSpigot.getInstance().getConfig();
        int minPasswordLength = config.getInt("Main-Settings.Password-Requirements.Minimum-length");
        if (password.length() < minPasswordLength) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.password-too-short")
                    .replace("{min-length}", String.valueOf(minPasswordLength)));
            return;
        }

        if (isPasswordTooSimple(password)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.password-too-simple"));
            return;
        }

        PasswordSecurity.encodeAsync(MainSpigot.getInstance(), password, encodedPassword -> {
            PlayerDataSave.savePlayerData(player, encodedPassword);
            PlayerRestrictions.unblockPlayer(player);
            PlayerDataSave.setPlayerSession(player.getName(), false);
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.registration-success"));
            LoginMessagesManager.CrackedRegisterTitle(player);
        });
    }

    public static boolean isPlayerRegistered(Player player) {
        return PlayerDataSave.loadPlayerData(player.getUniqueId()) != null;
    }

    public static boolean isPasswordTooSimple(String password) {
        boolean passwordSimple = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Password-Requirements.Password-simple");
        if (!passwordSimple) {
            return false;
        }
        File file = new File("plugins/tomekf1846-Login/blocked-passwords.yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration blockedPasswordsConfig = YamlConfiguration.loadConfiguration(file);
        List<String> blockedPasswords = blockedPasswordsConfig.getStringList("blocked_passwords");
        return blockedPasswords.stream().anyMatch(p -> p.equalsIgnoreCase(password));
    }
}
