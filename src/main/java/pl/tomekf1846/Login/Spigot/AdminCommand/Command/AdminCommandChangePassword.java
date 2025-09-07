package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.UUID;

public class AdminCommandChangePassword {
    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());

    public static void changePassword(CommandSender sender, String playerName, String newPassword) {
        Player player = Bukkit.getPlayer(playerName);
        UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);
        String prefix = languageManager.getMessage("messages.prefix.main-prefix");

        if (uuid == null) {
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.player_not_found"));
            return;
        }
        if (PlayerDataSave.isPlayerPremium(playerName)) {
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.cannot_change_premium")
                    .replace("{player}", playerName));
            return;
        }

        PlayerLoginManager.removePlayerLoginStatus(player);
        PlayerDataSave.setPlayerPassword(uuid, newPassword);
        sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.password_changed")
                .replace("{player}", playerName));
    }
}
