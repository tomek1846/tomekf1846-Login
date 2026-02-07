package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.MainSpigot;


public class AdminCommandForceLogin {

    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());


    public static void forceLogin(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");

        if (player == null || !player.isOnline()) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.player_not_online").replace("{player}", playerName));
            return;
        }

        if (!PlayerRestrictions.isPlayerBlocked(player)) {
            sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.already_logged_in").replace("{player}", player.getName()));
            return;
        }

        PlayerRestrictions.unblockPlayer(player);
        PlayerDataSave.saveLoginAttempt(player, true, null, 0);
        LoginMessagesManager.LoginAdminTitle(player);
        player.sendMessage(LanguageManager.getMessage(player, "messages.prefix.main-prefix")
                + LanguageManager.getMessage(player, "messages.player-commands.successfully_logged_in"));
        sender.sendMessage(prefix + LanguageManager.getMessage(sender, "messages.admin-commands.force_login_success").replace("{player}", player.getName()));

    }
}
