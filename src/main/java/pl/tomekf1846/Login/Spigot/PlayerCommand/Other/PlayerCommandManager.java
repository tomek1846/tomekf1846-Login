package pl.tomekf1846.Login.Spigot.PlayerCommand.Other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.PlayerCommand.Command.PlayerCommandChangePassword;
import pl.tomekf1846.Login.Spigot.PlayerCommand.Command.PlayerCommandEmail;
import pl.tomekf1846.Login.Spigot.PlayerCommand.Command.PlayerCommandPremiumCracked;

public class PlayerCommandManager implements CommandExecutor {

    public PlayerCommandManager() {}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getMessage("messages.prefix.main-prefix")
                    + LanguageManager.getMessage("messages.player-commands.only_players"));
            return true;
        }

        boolean isPremiumCommandEnabled = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Premium-Command", true);
        if (!isPremiumCommandEnabled && (label.equalsIgnoreCase("premium") || label.equalsIgnoreCase("cracked"))) {
            player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.command_disabled"));
            return true;
        }

        switch (label.toLowerCase()) {
            case "register":
            case "reg":
                if (args.length < 2) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.register_usage"));
                    return true;
                }
                PlayerRegisterManager.registerPlayer(player, args[0], args[1]);
                return true;

            case "login":
            case "log":
            case "l":
                if (args.length < 1) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.login_usage"));
                    return true;
                }
                PlayerLoginManager.loginPlayer(player, args[0]);
                return true;

            case "premium":
                if (args.length < 1) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.premium_usage"));
                    return true;
                }
                PlayerCommandPremiumCracked.handlePremiumCommand(player, args[0]);
                return true;

            case "cracked":
                if (args.length < 1) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.cracked_usage"));
                    return true;
                }
                PlayerCommandPremiumCracked.handleCrackedCommand(player, args[0]);
                return true;

            case "changepass":
            case "changepassword":
                if (args.length < 2) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.changepass_usage"));
                    return true;
                }
                PlayerCommandChangePassword.changePassword(player, args[0], args[1]);
                return true;

            case "email":
                if (args.length < 1) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.email_usage"));
                    return true;
                }
                PlayerCommandEmail.changeEmail(player, player.getName(), args[0]);
                return true;

            case "login-help":
            case "ulogin":
            case "userlogin":
                sendUsage(player);
                return true;

            case "language":
            case "lang":
                if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
                    pl.tomekf1846.Login.Spigot.GUI.Language.LanguageGui.openGUI(player);
                    return true;
                }
                String languageKey = LanguageManager.resolveLanguageKey(args[0]);
                if (languageKey == null) {
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.language_not_found"));
                    return true;
                }
                pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave.setPlayerLanguage(player.getUniqueId(), languageKey);
                player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.language_changed")
                        .replace("{language}", LanguageManager.getMessage(player, "messages.languages." + languageKey + ".name")));
                return true;

            default:
                player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.unknown_command"));
                return false;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.available_commands"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.login_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.log_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.short_login_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.register_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.short_register_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.cracked_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.premium_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.changepass_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.changepassword_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.email_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.language_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.login_help_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.userlogin_command"));
        player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.help.ulogin_command"));
    }

    private String getPrefix(Player player) {
        return LanguageManager.getMessage(player, "messages.prefix.main-prefix");
    }
}
