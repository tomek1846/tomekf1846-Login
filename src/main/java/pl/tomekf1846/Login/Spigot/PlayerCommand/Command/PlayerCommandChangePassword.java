package pl.tomekf1846.Login.Spigot.PlayerCommand.Command;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.Map;
import java.util.UUID;

import static pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager.isPasswordTooSimple;

public class PlayerCommandChangePassword {
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");

    public static void changePassword(CommandSender sender, String oldPassword, String newPassword) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.only_players"));
            return;
        }

        String playerName = player.getName();

        if (PlayerDataSave.isPlayerPremium(playerName)) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.premium-player"));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Map<String, String> playerData = PlayerDataSave.loadPlayerData(playerUUID);
        if (playerData == null) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.data-not-found"));
            return;
        }

        String storedPassword = playerData.get("Password");
        if (storedPassword == null || !storedPassword.equals(oldPassword)) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.incorrect-old-password"));
            return;
        }

        FileConfiguration config = MainSpigot.getInstance().getConfig();
        int minPasswordLength = config.getInt("Main-Settings.Password-Requirements.Minimum-length");
        if (newPassword.length() < minPasswordLength) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.password-too-short").replace("{min-length}", String.valueOf(minPasswordLength)));
            return;
        }

        if (isPasswordTooSimple(newPassword)) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.password-too-simple"));
            return;
        }

        PlayerDataSave.setPlayerPassword(playerUUID, newPassword);
        PlayerLoginManager.removePlayerLoginStatus(player);
        SessionCrackedManager.clearLoginCount(player.getUniqueId());
        sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.success-password-change"));
    }
}
