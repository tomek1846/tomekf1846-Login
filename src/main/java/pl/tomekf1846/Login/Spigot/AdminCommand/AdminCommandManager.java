package pl.tomekf1846.Login.Spigot.AdminCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.tomekf1846.Login.Spigot.AdminCommand.Command.*;
import pl.tomekf1846.Login.Spigot.GUI.MainGui.MainGui;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public class AdminCommandManager implements CommandExecutor {

    private final AdminCommandAbout commandAbout;
    private final LanguageManager languageManager;

    public AdminCommandManager(Plugin plugin, LanguageManager languageManager) {
        this.commandAbout = new AdminCommandAbout(plugin, languageManager);
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");

        switch (args[0].toLowerCase()) {
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.only_players_gui"));
                    return true;
                }
                MainGui.openGUI((Player) sender);
                return true;

            case "about":
                return commandAbout.onCommand(sender, command, label, args);

            case "forcelogin":
                if (args.length < 2) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_forcelogin"));
                    return true;
                }
                AdminCommandForceLogin.forceLogin(sender, args[1]);
                return true;

            case "cracked":
                if (args.length < 3) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_cracked"));
                    return true;
                }
                AdminCommandPremiumCracked.setPlayerCracked(sender, args[1], args[2]);
                return true;

            case "premium":
                if (args.length < 2) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_premium"));
                    return true;
                }
                AdminCommandPremiumCracked.setPlayerPremium(sender, args[1]);
                return true;

            case "ip":
                if (args.length < 2) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_ip"));
                    return true;
                }
                sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.player_ip_list").replace("{player}", args[1]));
                return true;

            case "changepass":
            case "changepassword":
                if (args.length < 3) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_changepass"));
                    return true;
                }
                AdminCommandChangePassword.changePassword(sender, args[1], args[2]);
                return true;

            case "email":
                if (args.length < 3) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_email"));
                    return true;
                }
                AdminCommandEmail.changeEmail(sender, args[1], args[2]);
                return true;

            case "register":
                if (args.length < 3) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_register"));
                    return true;
                }
                AdminCommandRegister.forceRegister(sender, args[1], args[2]);
                return true;

            case "unregister":
                if (args.length < 2) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_unregister"));
                    return true;
                }
                AdminCommandUnregister.unregisterPlayer(sender, args[1]);
                return true;

            case "language":
            case "lang":
                if (args.length < 3) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_language"));
                    return true;
                }
                AdminCommandLanguage.setLanguage(sender, args[1], args[2]);
                return true;

            case "reload":
                if (args.length > 1) {
                    sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.usage_reload"));
                    return true;
                }
                MainSpigot.getInstance().reloadConfig();
                SessionCrackedManager.clearLoginCounts();
                PlayerRestrictions.reloadAllowedCommands();
                LanguageManager.configReload(MainSpigot.getInstance());
                sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.reload_success"));
                return true;

            default:
                sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.unknown_command"));
                return false;
        }
    }

    private void sendUsage(CommandSender sender) {
        String prefix = LanguageManager.getMessage(sender, "messages.prefix.main-prefix");
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.available_commands"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.about_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.gui_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.cracked_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.premium_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.changepass_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.unregister_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.register_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.email_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.forcelogin_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.ip_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.language_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.lang_command"));
        sender.sendMessage(prefix + languageManager.getMessage(sender, "messages.admin-commands.help.reload_command"));
    }
}
