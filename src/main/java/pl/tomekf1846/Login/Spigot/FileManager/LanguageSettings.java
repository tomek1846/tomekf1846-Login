package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LanguageSettings {
    private LanguageSettings() {}

    public static Map<String, LanguageOption> getLanguageOptions() {
        FileConfiguration config = MainSpigot.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("Main-Settings.Language-Options");
        Map<String, LanguageOption> options = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String normalized = normalizeKey(key);
                List<String> aliases = section.getStringList(key + ".Aliases");
                String texture = section.getString(key + ".Skull-Texture", "");
                String owner = section.getString(key + ".Skull-Owner", "");
                options.put(normalized, new LanguageOption(key, aliases, texture, owner));
            }
        }
        if (options.isEmpty()) {
            options.put("english", new LanguageOption("English", List.of("english", "en"), "", ""));
            options.put("polish", new LanguageOption("Polish", List.of("polish", "pl", "polski"), "", ""));
            options.put("spanish", new LanguageOption("Spanish", List.of("spanish", "es", "espanol", "español"), "", ""));
            options.put("chinese", new LanguageOption("Chinese", List.of("chinese", "zh", "中文"), "", ""));
            options.put("german", new LanguageOption("German", List.of("german", "de", "deutsch"), "", ""));
            options.put("french", new LanguageOption("French", List.of("french", "fr", "francais", "français"), "", ""));
            options.put("portuguese", new LanguageOption("Portuguese", List.of("portuguese", "pt", "portugues", "português", "pt-br", "pt-pt"), "", ""));
        }
        return options;
    }

    public static List<String> getLanguageCommandNames() {
        List<String> names = new ArrayList<>();
        for (LanguageOption option : getLanguageOptions().values()) {
            names.add(option.commandName());
        }
        return names;
    }

    public static List<String> getAllLanguageAliases() {
        List<String> aliases = new ArrayList<>();
        for (Map.Entry<String, LanguageOption> entry : getLanguageOptions().entrySet()) {
            aliases.add(entry.getKey());
            aliases.addAll(entry.getValue().aliases());
        }
        return aliases;
    }

    public static String normalizeLanguage(String input) {
        if (input == null || input.isBlank()) {
            return "english";
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, LanguageOption> entry : getLanguageOptions().entrySet()) {
            if (entry.getKey().equals(trimmed)) {
                return entry.getKey();
            }
            for (String alias : entry.getValue().aliases()) {
                if (trimmed.equals(alias.toLowerCase(Locale.ROOT))) {
                    return entry.getKey();
                }
            }
        }
        return trimmed;
    }

    private static String normalizeKey(String input) {
        if (input == null) {
            return "english";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }

    public static LanguageOption getLanguageOption(String key) {
        if (key == null) {
            return null;
        }
        return getLanguageOptions().get(normalizeLanguage(key));
    }

    public record LanguageOption(String commandName, List<String> aliases, String texture, String owner) {
        public List<String> aliases() {
            if (aliases == null) {
                return Collections.emptyList();
            }
            return aliases;
        }
    }
}
