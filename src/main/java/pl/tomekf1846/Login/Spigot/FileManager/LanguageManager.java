package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class LanguageManager {
    private final JavaPlugin plugin;
    private static JavaPlugin pluginInstance;
    private static final java.util.Map<String, FileConfiguration> languageConfigs = new java.util.HashMap<>();
    private static String defaultLanguage = "english";

    private static final String ENGLISH_FILE_NAME = "messages-en.yml";
    private static final String POLISH_FILE_NAME = "messages-pl.yml";
    private static final String SPANISH_FILE_NAME = "messages-es.yml";
    private static final String CHINESE_FILE_NAME = "messages-zh.yml";

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        pluginInstance = plugin;
        loadLanguageFile();
    }

    private void saveDefaultLanguageFile(String fileName) {
        File file = new File(plugin.getDataFolder(), "Languages/" + fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource(fileName);
                 OutputStream out = Files.newOutputStream(file.toPath())) {
                if (in != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("File creation error " + fileName + ": " + e.getMessage());
            }
        }
    }

    public void loadLanguageFile() {
        File languagesDir = new File(plugin.getDataFolder(), "Languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        saveDefaultLanguageFile(ENGLISH_FILE_NAME);
        saveDefaultLanguageFile(POLISH_FILE_NAME);
        saveDefaultLanguageFile(SPANISH_FILE_NAME);
        saveDefaultLanguageFile(CHINESE_FILE_NAME);

        String language = plugin.getConfig().getString("Main-Settings.Language");
        defaultLanguage = LanguageSettings.normalizeLanguage(Objects.requireNonNullElse(language, "english"));
        languageConfigs.clear();
        loadLanguageConfig(languagesDir, "english", ENGLISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "polish", POLISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "spanish", SPANISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "chinese", CHINESE_FILE_NAME);
    }

    public static void configReload(JavaPlugin plugin) {
        plugin.reloadConfig();
        LanguageManager instance = new LanguageManager(plugin);
        instance.loadLanguageFile();
    }

    @Contract(pure = true)
    private static @NotNull String convertColors(String message) {
        return message != null ? message.replace("&", "§") : "";
    }

    public static @NotNull String getMessage(String path) {
        return getMessage(null, path);
    }

    public static @NotNull String getMessage(org.bukkit.command.CommandSender sender, String path) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return getMessage(player, path);
        }
        return getMessageFromLanguage(defaultLanguage, path);
    }

    public static @NotNull String getMessage(org.bukkit.entity.Player player, String path) {
        String language = defaultLanguage;
        if (player != null) {
            String playerLanguage = PlayerDataSave.getPlayerLanguage(player.getUniqueId());
            if (playerLanguage != null && !playerLanguage.isBlank()) {
                language = LanguageSettings.normalizeLanguage(playerLanguage);
            }
        }
        return getMessageFromLanguage(language, path);
    }

    public static @NotNull List<String> getMessageList(String path) {
        return getMessageList(null, path);
    }

    public static @NotNull List<String> getMessageList(org.bukkit.entity.Player player, String path) {
        String language = defaultLanguage;
        if (player != null) {
            String playerLanguage = PlayerDataSave.getPlayerLanguage(player.getUniqueId());
            if (playerLanguage != null && !playerLanguage.isBlank()) {
                language = LanguageSettings.normalizeLanguage(playerLanguage);
            }
        }
        return getMessageListFromLanguage(language, path);
    }

    private static void loadLanguageConfig(File languagesDir, String key, String fileName) {
        File languageFile = new File(languagesDir, fileName);
        if (!languageFile.exists()) {
            pluginInstance.getLogger().severe("File " + fileName + " not found!");
            return;
        }
        languageConfigs.put(key, YamlConfiguration.loadConfiguration(languageFile));
    }

    private static @NotNull String getMessageFromLanguage(String language, String path) {
        FileConfiguration config = languageConfigs.getOrDefault(language, languageConfigs.get("english"));
        if (config == null) {
            return convertColors("&cMessages error: " + path);
        }
        String message = config.getString(path, "&cMessages error: " + path);
        return convertColors(message);
    }

    private static @NotNull List<String> getMessageListFromLanguage(String language, String path) {
        FileConfiguration config = languageConfigs.getOrDefault(language, languageConfigs.get("english"));
        if (config == null) {
            return List.of();
        }
        List<String> messages = config.getStringList(path);
        return messages.stream()
                .map(LanguageManager::convertColors)
                .toList();
    }
}
}
