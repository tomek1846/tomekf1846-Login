package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.MainSpigot;


public class AdminCommandForceLogin {

    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());


    public static void forceLogin(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        String prefix = languageManager.getMessage("messages.prefix.main-prefix");

        if (player == null || !player.isOnline()) {
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.player_not_online").replace("{player}", playerName));
            return;
        }

        if (!PlayerRestrictions.isPlayerBlocked(player)) {
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.already_logged_in").replace("{player}", player.getName()));
            return;
        }

        PlayerRestrictions.unblockPlayer(player);
        LoginMessagesManager.LoginAdminTitle(player);
        player.sendMessage(prefix + languageManager.getMessage("messages.player-commands.successfully_logged_in"));
        sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.force_login_success").replace("{player}", player.getName()));

    }
}
