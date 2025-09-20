package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.*;

import pl.tomekf1846.Login.Spigot.MainSpigot;

public class MojangAuthService {

    public static MojangProfile queryHasJoined(String username, String serverHash) {
        final int maxAttempts = 10;
        final long defaultDelay = 200L;
        final long rateLimitDelay = 400L;
        String pluginName = MainSpigot.getInstance().getDescription().getName();
        String pluginVersion = MainSpigot.getInstance().getDescription().getVersion();
        String userAgent = pluginName + "/" + pluginVersion;

        try {
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                String urlStr = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                        + java.net.URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&serverId=" + java.net.URLEncoder.encode(serverHash, StandardCharsets.UTF_8);

                HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
                con.setConnectTimeout(7000);
                con.setReadTimeout(7000);
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", userAgent);

                try {
                    int code = con.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        JsonObject obj;
                        try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                            obj = JsonParser.parseReader(reader).getAsJsonObject();
                        }

                        if (!obj.has("id")) return null;
                        String id = obj.get("id").getAsString();
                        String name = obj.has("name") ? obj.get("name").getAsString() : username;

                        String s = id.trim();
                        if (s.length() != 32) return null;
                        String dashed = s.replaceFirst(
                                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                                "$1-$2-$3-$4-$5"
                        );
                        UUID uuid = UUID.fromString(dashed);

                        List<Map<String, String>> propsList = new ArrayList<>();
                        if (obj.has("properties")) {
                            JsonArray props = obj.getAsJsonArray("properties");
                            for (int i = 0; i < props.size(); i++) {
                                JsonObject p = props.get(i).getAsJsonObject();
                                Map<String, String> m = new HashMap<>();
                                m.put("name", p.get("name").getAsString());
                                m.put("value", p.get("value").getAsString());
                                if (p.has("signature")) m.put("signature", p.get("signature").getAsString());
                                propsList.add(m);
                            }
                        }
                        return new MojangProfile(uuid, name, propsList);
                    }

                    long sleepDelay = defaultDelay;
                    boolean retry;
                    if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                        // 204 oznacza, że gracz jeszcze nie zakończył logowania - spróbuj ponownie
                        retry = true;
                    } else if (code == HttpURLConnection.HTTP_FORBIDDEN || code == 429) {
                        System.out.println("[PremiumLogin] hasJoined HTTP code=" + code + " (rate limited/forbidden)");
                        retry = true;
                        sleepDelay = rateLimitDelay;
                    } else if (code >= 500 && code < 600) {
                        System.out.println("[PremiumLogin] hasJoined temporary server error code=" + code);
                        retry = true;
                    } else {
                        System.out.println("[PremiumLogin] hasJoined unexpected HTTP code=" + code);
                        retry = false;
                    }

                    if (!retry) {
                        return null;
                    }

                    if (attempt < maxAttempts - 1) {
                        Thread.sleep(sleepDelay);
                        continue;
                    }
                } finally {
                    con.disconnect();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String computeServerHash(byte[] sharedSecret, PublicKey publicKey) throws Exception {
        final String serverId = "";

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        sha1.update(sharedSecret);
        sha1.update(publicKey.getEncoded());

        byte[] digest = sha1.digest();
        return toMinecraftHex(digest);
    }

    private static String toMinecraftHex(byte[] digest) {
        if (digest.length == 0) {
            return "0";
        }

        return new BigInteger(digest).toString(16);
    }
}
