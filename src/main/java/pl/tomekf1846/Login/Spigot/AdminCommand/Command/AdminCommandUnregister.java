package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.io.File;
import java.util.UUID;

public class AdminCommandUnregister {
    private static final String DATA_FOLDER = "plugins/tomekf1846-Login/Data";
    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());
    private static final String PREFIX = languageManager.getMessage("messages.prefix.main-prefix");


    public static boolean unregisterPlayer(CommandSender sender, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (sender instanceof Player && sender.getName().equalsIgnoreCase(playerName)) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.cannot_unregister_self"));
            return false;
        }

        if (targetPlayer == null) {
            UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);

            if (uuid == null) {
                sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.player_does_not_exist").replace("{player}", playerName));
                return false;
            }
            return unregisterOfflinePlayer(sender, uuid, playerName);
        }

        UUID uuid = targetPlayer.getUniqueId();
        return unregisterOnlinePlayer(sender, targetPlayer, uuid, playerName);
    }

    private static boolean unregisterOnlinePlayer(CommandSender sender, Player targetPlayer, UUID uuid, String playerName) {
        File playerFile = new File(DATA_FOLDER, uuid + ".yml");

        if (playerFile.exists()) {
            if (playerFile.delete()) {
                targetPlayer.kickPlayer(languageManager.getMessage("messages.admin-commands.player_kicked_unregistered"));
                sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.successfully_unregistered").replace("{player}", playerName));
                return true;
            }
        }
        sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.failed_to_unregister").replace("{player}", playerName));
        PlayerLoginManager.removePlayerLoginStatus(targetPlayer);
        return false;
    }

    private static boolean unregisterOfflinePlayer(CommandSender sender, UUID uuid, String playerName) {
        File playerFile = new File(DATA_FOLDER, uuid + ".yml");

        if (playerFile.exists()) {
            if (playerFile.delete()) {
                sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.successfully_unregistered").replace("{player}", playerName));
                return true;
            }
        }
        sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.failed_to_unregister").replace("{player}", playerName));
        return false;
    }
}
