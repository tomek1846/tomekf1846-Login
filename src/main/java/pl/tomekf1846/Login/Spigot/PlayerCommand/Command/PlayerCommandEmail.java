package pl.tomekf1846.Login.Spigot.PlayerCommand.Command;

import org.bukkit.command.CommandSender;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.UUID;

public class PlayerCommandEmail {
    public static void changeEmail(CommandSender sender, String playerName, String newEmail) {
        String prefix = sender instanceof org.bukkit.entity.Player player
                ? LanguageManager.getMessage(player, "messages.prefix.main-prefix")
                : LanguageManager.getMessage("messages.prefix.main-prefix");
        UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);
        if (uuid == null) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.player_not_found"));
            return;
        }

        int atIndex = newEmail.indexOf("@");
        int lastDotIndex = newEmail.lastIndexOf(".");
        if (newEmail.length() < 3 ||
                !newEmail.contains("@") ||
                newEmail.chars().filter(ch -> ch == '@').count() > 1 ||
                newEmail.startsWith("@") ||
                newEmail.contains("@.") ||
                lastDotIndex == -1 ||
                atIndex > lastDotIndex ||
                lastDotIndex - atIndex < 3
        ) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.player-commands.invalid-email-format"));
            return;
        }

        PlayerDataSave.setPlayerEmail(uuid, newEmail);
        sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.email_changed")
                .replace("{player}", playerName)
                .replace("{email}", newEmail));
    }
}
