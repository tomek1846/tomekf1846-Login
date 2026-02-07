package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LanguageManager {
    private final JavaPlugin plugin;
    private static final Map<String, FileConfiguration> LANGUAGE_CONFIGS = new HashMap<>();
    private static String defaultLanguageKey = "english";

    private static final String ENGLISH_FILE_NAME = "messages-en.yml";
    private static final String POLISH_FILE_NAME = "messages-pl.yml";
    private static final String SPANISH_FILE_NAME = "messages-es.yml";
    private static final String CHINESE_FILE_NAME = "messages-zh.yml";

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
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

        defaultLanguageKey = normalizeLanguageKey(plugin.getConfig().getString("Main-Settings.Language"));
        loadLanguageConfig(languagesDir, "english", ENGLISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "polish", POLISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "spanish", SPANISH_FILE_NAME);
        loadLanguageConfig(languagesDir, "chinese", CHINESE_FILE_NAME);
    }

    private void loadLanguageConfig(File languagesDir, String key, String fileName) {
        File languageFile = new File(languagesDir, fileName);
        if (!languageFile.exists()) {
            plugin.getLogger().severe("File " + fileName + " not found!");
            return;
        }
        LANGUAGE_CONFIGS.put(key, YamlConfiguration.loadConfiguration(languageFile));
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
        return getMessage(defaultLanguageKey, path);
    }

    public static @NotNull String getMessage(CommandSender sender, String path) {
        if (sender instanceof Player player) {
            return getMessage(player, path);
        }
        return getMessage(defaultLanguageKey, path);
    }

    public static @NotNull String getMessage(Player player, String path) {
        String languageKey = getPlayerLanguageKey(player);
        return getMessage(languageKey, path);
    }

    public static @NotNull List<String> getMessageList(String path) {
        return getMessageList(defaultLanguageKey, path);
    }

    public static @NotNull List<String> getMessageList(CommandSender sender, String path) {
        if (sender instanceof Player player) {
            return getMessageList(player, path);
        }
        return getMessageList(defaultLanguageKey, path);
    }

    public static @NotNull List<String> getMessageList(Player player, String path) {
        return getMessageList(getPlayerLanguageKey(player), path);
    }

    public static @NotNull String getDefaultLanguageKey() {
        return defaultLanguageKey;
    }

    public static @NotNull List<String> getSupportedLanguageKeys() {
        return List.of("english", "polish", "spanish", "chinese");
    }

    public static String normalizeLanguageKey(String input) {
        if (input == null) {
            return defaultLanguageKey;
        }
        String normalized = input.trim().toLowerCase();
        if (getSupportedLanguageKeys().contains(normalized)) {
            return normalized;
        }
        return defaultLanguageKey;
    }

    public static String resolveLanguageKey(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toLowerCase();
        return getSupportedLanguageKeys().contains(normalized) ? normalized : null;
    }

    private static String getPlayerLanguageKey(Player player) {
        if (player == null) {
            return defaultLanguageKey;
        }
        String languageKey = PlayerDataSave.getPlayerLanguage(player.getUniqueId());
        return languageKey == null || languageKey.isEmpty() ? defaultLanguageKey : languageKey;
    }

    private static @NotNull String getMessage(String languageKey, String path) {
        FileConfiguration config = LANGUAGE_CONFIGS.getOrDefault(languageKey, LANGUAGE_CONFIGS.get(defaultLanguageKey));
        if (config == null) {
            return convertColors("&cMessages error: " + path);
        }
        String message = config.getString(path, "&cMessages error: " + path);
        return convertColors(message);
    }

    private static @NotNull List<String> getMessageList(String languageKey, String path) {
        FileConfiguration config = LANGUAGE_CONFIGS.getOrDefault(languageKey, LANGUAGE_CONFIGS.get(defaultLanguageKey));
        if (config == null) {
            return List.of(convertColors("&cMessages error: " + path));
        }
        List<String> messages = config.getStringList(path);
        return messages.stream()
                .map(LanguageManager::convertColors)
                .toList();
    }
}
