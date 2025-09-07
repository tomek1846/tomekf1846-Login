package pl.tomekf1846.Login.Spigot.PlayerCommand.Command;

import org.bukkit.command.CommandSender;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.UUID;

public class PlayerCommandEmail {
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");

    public static void changeEmail(CommandSender sender, String playerName, String newEmail) {
        UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);
        if (uuid == null) {
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.admin-commands.player_not_found"));
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
            sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.invalid-email-format"));
            return;
        }

        PlayerDataSave.setPlayerEmail(uuid, newEmail);
        sender.sendMessage(PREFIX + LanguageManager.getMessage("messages.admin-commands.email_changed")
                .replace("{player}", playerName)
                .replace("{email}", newEmail));
    }
}
