package pl.tomekf1846.Login.Spigot.AdminCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public class AdminCommandAbout implements CommandExecutor {

    private final Plugin plugin;
    private final LanguageManager languageManager;

    public AdminCommandAbout(Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        sendPluginInfo(sender);
        return true;
    }

    private void sendPluginInfo(CommandSender sender) {
        String prefix = languageManager.getMessage("messages.prefix.main-prefix");
        String version = plugin.getDescription().getVersion();
        String author = plugin.getDescription().getAuthors().isEmpty() ? "Unknown" : String.join(", ", plugin.getDescription().getAuthors());
        String website = plugin.getDescription().getWebsite() != null ? plugin.getDescription().getWebsite() : "None";

        sender.sendMessage(prefix + languageManager.getMessage("messages.startmessages.plugin_info.info"));
        sender.sendMessage(prefix + languageManager.getMessage("messages.startmessages.plugin_info.version_prefix") + version);
        sender.sendMessage(prefix + languageManager.getMessage("messages.startmessages.plugin_info.author_prefix") + author);
        sender.sendMessage(prefix + languageManager.getMessage("messages.startmessages.plugin_info.website_prefix") + website);
    }
}
