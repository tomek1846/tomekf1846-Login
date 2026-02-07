package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LanguageAutoDetect {
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;

    private LanguageAutoDetect() {}

    public static void applyAutoDetectOnFirstJoin(Player player) {
        if (player == null) {
            return;
        }
        if (!LanguageSettings.isPerPlayerLanguageEnabled()
                || !LanguageSettings.isAutoDetectOnFirstJoinEnabled()) {
            return;
        }
        String currentLanguage = PlayerDataSave.getPlayerLanguage(player.getUniqueId());
        if (currentLanguage != null && !currentLanguage.isBlank()) {
            return;
        }
        String ipAddress = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : null;
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        String countryCode = lookupCountryCode(ipAddress);
        if (countryCode == null || countryCode.isBlank()) {
            return;
        }
        String languageKey = mapCountryToLanguage(countryCode);
        if (languageKey == null || languageKey.isBlank()) {
            return;
        }
        Map<String, LanguageSettings.LanguageOption> options = LanguageSettings.getLanguageOptions();
        if (!options.containsKey(languageKey)) {
            return;
        }
        PlayerDataSave.setPlayerLanguage(player.getUniqueId(), languageKey);
        String languageName = languageKey;
        LanguageSettings.LanguageOption option = options.get(languageKey);
        if (option != null) {
            languageName = option.commandName();
        }
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.language_auto_detected")
                .replace("{language}", languageName));
    }

    private static String lookupCountryCode(String ipAddress) {
        String template = MainSpigot.getInstance().getConfig().getString(
                "Main-Settings.Language-GeoIP-Url",
                "https://ipapi.co/{ip}/country/"
        );
        String target = template.replace("{ip}", ipAddress);
        try {
            URL url = new URL(target);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.readLine();
                if (response == null) {
                    return null;
                }
                String trimmed = response.trim().toUpperCase(Locale.ROOT);
                if (trimmed.length() != 2) {
                    return null;
                }
                return trimmed;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String mapCountryToLanguage(String countryCode) {
        String code = countryCode.toUpperCase(Locale.ROOT);
        Set<String> spanish = Set.of(
                "ES", "MX", "AR", "CL", "CO", "PE", "VE", "EC", "GT",
                "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR", "PA",
                "UY", "GQ"
        );
        if (spanish.contains(code)) {
            return "spanish";
        }
        Set<String> portuguese = Set.of("PT", "BR");
        if (portuguese.contains(code)) {
            return "portuguese";
        }
        Set<String> french = Set.of("FR", "BE", "CA", "CH", "LU");
        if (french.contains(code)) {
            return "french";
        }
        Set<String> german = Set.of("DE", "AT", "CH");
        if (german.contains(code)) {
            return "german";
        }
        Set<String> chinese = Set.of("CN", "TW", "HK", "MO");
        if (chinese.contains(code)) {
            return "chinese";
        }
        if ("PL".equals(code)) {
            return "polish";
        }
        if ("EN".equals(code) || Set.of("US", "GB", "AU", "NZ", "IE").contains(code)) {
            return "english";
        }
        return null;
    }
}
