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
    private static FileConfiguration languageConfig;

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

        String language = plugin.getConfig().getString("Main-Settings.Language");
        String fileName = switch (Objects.requireNonNull(language).toLowerCase()) {
            case "polish" -> POLISH_FILE_NAME;
            case "spanish" -> SPANISH_FILE_NAME;
            case "chinese" -> CHINESE_FILE_NAME;
            default -> ENGLISH_FILE_NAME;
        };

        File languageFile = new File(languagesDir, fileName);
        if (!languageFile.exists()) {
            plugin.getLogger().severe("File " + fileName + " not found!");
        } else {
            languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        }
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
        String message = languageConfig.getString(path, "&cMessages error: " + path);
        return convertColors(message);
    }

    public static @NotNull List<String> getMessageList(String path) {
        List<String> messages = languageConfig.getStringList(path);
        return messages.stream()
                .map(LanguageManager::convertColors)
                .toList();
    }
}
