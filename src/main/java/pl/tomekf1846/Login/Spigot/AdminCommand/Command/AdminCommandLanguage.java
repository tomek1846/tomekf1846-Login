package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageSettings;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

import java.util.UUID;

public class AdminCommandLanguage {

    public static void setLanguage(CommandSender sender, String playerName, String language) {
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");
        UUID uuid = PlayerDataSave.findUUIDByNick(playerName);
        if (uuid == null) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.player_not_found"));
            return;
        }
        String normalized = LanguageSettings.normalizeLanguage(language);
        if (!LanguageSettings.getLanguageOptions().containsKey(normalized)) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.language_unknown"));
            return;
        }

        PlayerDataSave.setPlayerLanguage(uuid, normalized);
        sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.language_changed")
                .replace("{player}", playerName)
                .replace("{language}", LanguageSettings.getLanguageOption(normalized).commandName()));

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            String targetPrefix = LanguageManager.getMessage(target, "messages.prefix.main-prefix");
            target.sendMessage(targetPrefix + LanguageManager.getMessage(target, "messages.player-commands.language_changed")
                    .replace("{language}", LanguageSettings.getLanguageOption(normalized).commandName()));
        }
    }
}
