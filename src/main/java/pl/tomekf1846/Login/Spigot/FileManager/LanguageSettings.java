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

    private static final Map<String, LanguageOption> DEFAULT_LANGUAGE_OPTIONS;

    static {
        Map<String, LanguageOption> defaults = new LinkedHashMap<>();
        defaults.put("english", new LanguageOption(
                "English",
                List.of("english", "en"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmNiYzMyY2IyNGQ1N2ZjZGMwMzFlODUxMjM1ZGEyZGFhZDNlMTkxNGI4NzA0M2JkMDEyNjMzZTZmMzJjNyJ9fX0=",
                ""
        ));
        defaults.put("polish", new LanguageOption(
                "Polish",
                List.of("polish", "pl", "polski"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTIxYjJhZjhkMjMyMjI4MmZjZTRhMWFhNGYyNTdhNTJiNjhlMjdlYjMzNGY0YTE4MWZkOTc2YmFlNmQ4ZWIifX19",
                ""
        ));
        defaults.put("spanish", new LanguageOption(
                "Spanish",
                List.of("spanish", "es", "espanol", "español"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzJiZDQ1MjE5ODMzMDllMGFkNzZjMWVlMjk4NzQyODc5NTdlYzNkOTZmOGQ4ODkzMjRkYThjODg3ZTQ4NWVhOCJ9fX0=",
                ""
        ));
        defaults.put("chinese", new LanguageOption(
                "Chinese",
                List.of("chinese", "zh", "中文"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Y5YmMwMzVjZGM4MGYxYWI1ZTExOThmMjlmM2FkM2ZkZDJiNDJkOWE2OWFlYjY0ZGU5OTA2ODE4MDBiOThkYyJ9fX0=",
                ""
        ));
        defaults.put("german", new LanguageOption(
                "German",
                List.of("german", "de", "deutsch"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ==",
                ""
        ));
        defaults.put("french", new LanguageOption(
                "French",
                List.of("french", "fr", "francais", "français"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTEyNjlhMDY3ZWUzN2U2MzYzNWNhMWU3MjNiNjc2ZjEzOWRjMmRiZGRmZjk2YmJmZWY5OWQ4YjM1Yzk5NmJjIn19fQ==",
                ""
        ));
        defaults.put("portuguese", new LanguageOption(
                "Portuguese",
                List.of("portuguese", "pt", "portugues", "português", "pt-br", "pt-pt"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWJkNTFmNDY5M2FmMTc0ZTZmZTE5NzkyMzNkMjNhNDBiYjk4NzM5OGUzODkxNjY1ZmFmZDJiYTU2N2I1YTUzYSJ9fX0=",
                ""
        ));
        DEFAULT_LANGUAGE_OPTIONS = Collections.unmodifiableMap(defaults);
    }

    public static boolean isPerPlayerLanguageEnabled() {
        return MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Language-Per-Player", true);
    }

    public static boolean isAutoDetectOnFirstJoinEnabled() {
        return MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Language-Auto-Detect-On-First-Join", true);
    }

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
            options.putAll(DEFAULT_LANGUAGE_OPTIONS);
        } else {
            for (Map.Entry<String, LanguageOption> entry : DEFAULT_LANGUAGE_OPTIONS.entrySet()) {
                options.putIfAbsent(entry.getKey(), entry.getValue());
            }
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
