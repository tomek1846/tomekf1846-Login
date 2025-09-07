package pl.tomekf1846.Login.Proxy.Bungee.PluginManager;

import pl.tomekf1846.Login.Proxy.Bungee.MainProxy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PluginStart {
    private final MainProxy plugin;
    private static final Map<String, String> StartTime = new HashMap<>();
    private final Logger logger;

    public static class AnsiColor {
        public static final String RESET = "\u001B[0m";
        public static final String YELLOW = "\u001B[33m";
        public static final String GOLD = "\u001B[38;5;214m";
        public static final String LIGHT_GREEN = "\u001B[92m";
        public static final String LIGHT_BLUE = "\u001B[94m";
        public static final String WHITE = "\u001B[37m";
        public static final String RED = "\u001B[31m";
    }

    public PluginStart(MainProxy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    private String getTimePrefix() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return "[" + sdf.format(new Date()) + " INFO] ";
    }

    private String stripAnsiColor(String text) {
        return text.replaceAll("\\u001B\\[[;\\u0030-9]*m", "");
    }

    private String centerText(String text) {
        int maxLength = 42;
        int padding = (maxLength - stripAnsiColor(text).length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, maxLength - padding - stripAnsiColor(text).length()));
    }

    private void printBanner(String title, String color, String borderColor) {
        String[] banner = {
                borderColor + "|==========================================|" + AnsiColor.RESET,
                borderColor + "|" + centerText(color + title + AnsiColor.RESET) + borderColor + "|" + AnsiColor.RESET,
                borderColor + "|==========================================|" + AnsiColor.RESET
        };
        for (String line : banner) {
            logger.info(getTimePrefix() + line);
        }
    }

    public void printPluginInfo() {
        String version = plugin.getDescription().getVersion();
        String[] banner = {
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.GOLD + "TOMEKF1846-LOGIN - PROXY VERSION" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_GREEN + "Plugin Info" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + "Version: " + version + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + "Author: tomekf1846" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|" + centerText(AnsiColor.LIGHT_BLUE + "Website: https://tomekf1846.pl" + AnsiColor.RESET) + AnsiColor.GOLD + "|" + AnsiColor.RESET,
                AnsiColor.GOLD + "|==========================================|" + AnsiColor.RESET
        };
        for (String line : banner) {
            logger.info(getTimePrefix() + line);
        }
    }

    public void printLoadingCommand() { printBanner("Loading Command", AnsiColor.GOLD, AnsiColor.GOLD); }
    public void printLoadingListener() { printBanner("Loading Listener", AnsiColor.GOLD, AnsiColor.GOLD); }
    public void printLoadingData() { printBanner("Loading Data", AnsiColor.GOLD, AnsiColor.GOLD); }
    public void printLoadingSuccess() { printBanner("Loaded Successfully!", AnsiColor.LIGHT_GREEN, AnsiColor.LIGHT_GREEN); }
    public void printPluginShutdown() { printBanner("Plugin Successfully Disabled", AnsiColor.LIGHT_GREEN, AnsiColor.LIGHT_GREEN); }
    public void printError() { printBanner("ERROR", AnsiColor.RED, AnsiColor.RED); }

    public static void StartTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(now);
        StartTime.put("start_time", formattedDate);
    }
}

