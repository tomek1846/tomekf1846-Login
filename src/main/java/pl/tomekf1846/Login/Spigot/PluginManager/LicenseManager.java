package pl.tomekf1846.Login.Spigot.PluginManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class LicenseManager {

    private pl.tomekf1846.API.License.LicenseManager licenseManager;
    private static final String PLUGIN_URL = "https://api.tomekf1846.com.pl/api/tomekf1846-API-1.2.3.jar";
    private static final String PLUGIN_NAME = "tomekf1846-API.jar";
    private static final String REQUIRED_VERSION = "123";

    public static void CheckLicense() {
        Plugin apiPlugin = Bukkit.getPluginManager().getPlugin("tomekf1846-API");
        if (apiPlugin == null) {
            Bukkit.getLogger().warning("tomekf1846-API not found. Downloading...");
            try {
                downloadAndInstallPlugin();
                loadPlugin(PLUGIN_NAME);
            } catch (IOException e) {
                throw new RuntimeException("Failed to download or install the plugin", e);
            }
        }

        if (pl.tomekf1846.API.License.LicenseManager.checkLicense()) {
            if (checkAPIPluginVersion()) {
                PluginStart.printlicenseAPI(); //WYJEBAC TO TU BĘDZIE COŚ JAK JEST OK LICENCJA NP. Sprawdzanie czy wersja jest wpsierana
            } else {
                throw new RuntimeException("The installed version of tomekf1846-API is too low for the plugin to work. Version 1.2.3 or newer is required.");
            }
        }
    }

    private static boolean checkAPIPluginVersion() {
        Plugin apiPlugin = Bukkit.getPluginManager().getPlugin("tomekf1846-API");
        if (apiPlugin == null) return false;

        String version = apiPlugin.getDescription().getVersion();
        return compareVersions(version.replace(".", ""), REQUIRED_VERSION) >= 0;
    }

    private static int compareVersions(String version1, String version2) {
        return Integer.compare(Integer.parseInt(version1), Integer.parseInt(version2));
    }

    private static void downloadAndInstallPlugin() throws IOException {
        URL url = new URL(PLUGIN_URL);
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = new FileOutputStream(new File(Bukkit.getServer().getPluginsFolder(), PLUGIN_NAME))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private static void loadPlugin(String pluginName) {
        File pluginFile = new File(Bukkit.getServer().getPluginsFolder(), pluginName);
        if (pluginFile.exists()) {
            PluginManager pluginManager = Bukkit.getPluginManager();
            try {
                Plugin plugin = pluginManager.loadPlugin(pluginFile);
                if (plugin != null) {
                    pluginManager.enablePlugin(plugin);
                }
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                Bukkit.getLogger().warning("Plug-in launch error: " + pluginName);
                e.printStackTrace();
            }
        }
    }
}