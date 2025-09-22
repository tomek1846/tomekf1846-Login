package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MojangAuthClient {

    private final String userAgent;

    public MojangAuthClient(Plugin plugin) {
        this.userAgent = plugin.getDescription().getName() + "/" + plugin.getDescription().getVersion();
    }

    public MojangProfile queryHasJoined(String username, String serverHash, String ip) {
        final int maxAttempts = 10;
        final long defaultDelay = 200L;
        final long rateLimitDelay = 400L;

        try {
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                HttpURLConnection connection = openConnection(username, serverHash, ip);
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", userAgent);

                try {
                    int code = connection.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        JsonObject obj;
                        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                            obj = JsonParser.parseReader(reader).getAsJsonObject();
                        }
                        return parseProfile(obj, username);
                    }

                    long sleepDelay = defaultDelay;
                    boolean retry;
                    if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                        retry = true;
                    } else if (code == HttpURLConnection.HTTP_FORBIDDEN || code == 429) {
                        retry = true;
                        sleepDelay = rateLimitDelay;
                    } else if (code >= 500 && code < 600) {
                        retry = true;
                    } else {
                        retry = false;
                    }

                    if (!retry) {
                        return null;
                    }

                    if (attempt < maxAttempts - 1) {
                        Thread.sleep(sleepDelay);
                    }
                } finally {
                    connection.disconnect();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpURLConnection openConnection(String username, String serverHash, String ip) throws Exception {
        StringBuilder url = new StringBuilder("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=")
                .append(URLEncoder.encode(username, StandardCharsets.UTF_8))
                .append("&serverId=")
                .append(URLEncoder.encode(serverHash, StandardCharsets.UTF_8));

        if (ip != null && !ip.isBlank()) {
            url.append("&ip=").append(URLEncoder.encode(ip, StandardCharsets.UTF_8));
        }

        return (HttpURLConnection) new URL(url.toString()).openConnection();
    }

    private MojangProfile parseProfile(JsonObject obj, String fallbackName) {
        if (obj == null || !obj.has("id")) {
            return null;
        }

        String id = obj.get("id").getAsString();
        if (id == null || id.length() != 32) {
            return null;
        }

        String name = obj.has("name") ? obj.get("name").getAsString() : fallbackName;

        String dashed = id.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})", "$1-$2-$3-$4-$5");
        UUID uuid = UUID.fromString(dashed);

        List<Map<String, String>> properties = new ArrayList<>();
        if (obj.has("properties")) {
            JsonArray props = obj.getAsJsonArray("properties");
            for (int i = 0; i < props.size(); i++) {
                JsonObject p = props.get(i).getAsJsonObject();
                Map<String, String> map = new HashMap<>();
                map.put("name", p.get("name").getAsString());
                map.put("value", p.get("value").getAsString());
                if (p.has("signature")) {
                    map.put("signature", p.get("signature").getAsString());
                }
                properties.add(map);
            }
        }

        return new MojangProfile(uuid, name, properties);
    }
}