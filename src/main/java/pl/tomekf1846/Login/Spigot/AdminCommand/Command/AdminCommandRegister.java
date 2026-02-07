package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.util.Map;

public class AdminCommandRegister {

    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());


    public static void forceRegister(CommandSender sender, String playerName, String password) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.never_played_before")
                    .replace("{player}", playerName));
            return;
        }
        Map<String, String> playerData = PlayerDataSave.loadPlayerData(offlinePlayer.getUniqueId());
        if (playerData != null) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.already_registered")
                    .replace("{player}", playerName));
            return;
        }
        PasswordSecurity.encodeAsync(MainSpigot.getInstance(), password, encodedPassword -> {
            PlayerDataSave.savePlayerData(offlinePlayer, encodedPassword);
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.successfully_registered")
                    .replace("{player}", playerName));
        });
    }
}
