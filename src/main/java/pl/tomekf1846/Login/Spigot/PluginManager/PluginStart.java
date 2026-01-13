package pl.tomekf1846.Login.Spigot.PluginManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PluginStart {
    private final JavaPlugin plugin;
    private static Logger logger;
    private static LanguageManager languageManager;
    private static final Map<String, String> StartTime = new HashMap<>();

    public static class AnsiColor {
        public static final String RESET = "\u001B[0m";
        public static final String YELLOW = "\u001B[33m";
        public static final String GOLD = "\u001B[38;5;214m";
        public static final String LIGHT_GREEN = "\u001B[92m";
        public static final String LIGHT_BLUE = "\u001B[94m";
        public static final String WHITE = "\u001B[37m";
        public static final String RED = "\u001B[31m";
    }

    public PluginStart(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        PluginStart.languageManager = languageManager;
    }

    private static String getTimePrefix() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return "[" + sdf.format(new Date()) + " INFO] ";
    }

    private static String stripAnsiColor(String text) {
        return text.replaceAll("\\u001B\\[[;\\u0030-9]*m", "");
    }

    private static String centerText(String text) {
        int maxLength = 42;
        int padding = (maxLength - stripAnsiColor(text).length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, maxLength - padding - stripAnsiColor(text).length()));
    }

    private static String applyFixedColors(String message) {
        return message
                .replace("{GOLD}", AnsiColor.GOLD)
                .replace("{LIGHT_GREEN}", AnsiColor.LIGHT_GREEN)
                .replace("{LIGHT_BLUE}", AnsiColor.LIGHT_BLUE)
                .replace("{RED}", AnsiColor.RED)
                .replace("{WHITE}", AnsiColor.WHITE)
                .replace("{YELLOW}", AnsiColor.YELLOW)
                .replace("{RESET}", AnsiColor.RESET);
    }

    private static void printBanner(String messageKey, String borderColor) {
        String message = applyFixedColors(languageManager.getMessage(messageKey));
        String border = applyFixedColors(borderColor);

        String[] banner = {
                border + "|==========================================|" + AnsiColor.RESET,
                border + "|" + centerText(message) + border + "|" + AnsiColor.RESET,
                border + "|==========================================|" + AnsiColor.RESET
        };

        for (String line : banner) {
            logger.info(getTimePrefix() + line);
        }
    }

    public void printPluginInfo() {
        String version = plugin.getDescription().getVersion();
        String info = languageManager.getMessage("messages.startmessages.plugin_info.info");
        String versionPrefix = languageManager.getMessage("messages.startmessages.plugin_info.version_prefix");
        String authorPrefix = languageManager.getMessage("messages.startmessages.plugin_info.author_prefix");
        String websitePrefix = languageManager.getMessage("messages.startmessages.plugin_info.website_prefix");

        String[] banner = {
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.GOLD + "TOMEKF1846-LOGIN - SPIGOT VERSION" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_GREEN + info + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + versionPrefix + version + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + authorPrefix + "tomekf1846" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + websitePrefix + "https://tomekf1846.pl" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET
        };

        for (String line : banner) {
            logger.info(getTimePrefix() + line);
        }
    }

    public void printLoadingCommand() { printBanner("messages.startmessages.banners.loading_command", AnsiColor.GOLD); }
    public void printLoadingListener() { printBanner("messages.startmessages.banners.loading_listener", AnsiColor.GOLD); }
    public void printLoadingData() { printBanner("messages.startmessages.banners.loading_data", AnsiColor.GOLD); }
    public void printLoadingSuccess() { printBanner("messages.startmessages.banners.loading_success", AnsiColor.LIGHT_GREEN); }
    public void printPluginShutdown() { printBanner("messages.startmessages.banners.plugin_shutdown", AnsiColor.LIGHT_GREEN); }
    public static void printError() { printBanner("messages.startmessages.banners.error", AnsiColor.RED); }

    public void startkickall() {
        String message = languageManager.getMessage("messages.startmessages.kick_messages.start");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(message);
        }
    }

    public void stopkickall() {
        String message = languageManager.getMessage("messages.startmessages.kick_messages.stop");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(message);
        }
    }

    public static void StartTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(now);
        StartTime.put("start_time", formattedDate);
    }

    public static String getServerStartTime() {
        return StartTime.get("start_time");
    }
}
