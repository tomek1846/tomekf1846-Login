package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.util.UUID;

public class AdminCommandChangePassword {
    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());

    public static void changePassword(CommandSender sender, String playerName, String newPassword) {
        Player player = Bukkit.getPlayer(playerName);
        UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");

        if (uuid == null) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.player_not_found"));
            return;
        }
        if (PlayerDataSave.isPlayerPremium(playerName)) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.cannot_change_premium")
                    .replace("{player}", playerName));
            return;
        }

        PlayerLoginManager.removePlayerLoginStatus(player);
        PasswordSecurity.encodeAsync(MainSpigot.getInstance(), newPassword, encodedPassword -> {
            PlayerDataSave.setPlayerPassword(uuid, encodedPassword);
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.password_changed")
                    .replace("{player}", playerName));
        });
    }
}
