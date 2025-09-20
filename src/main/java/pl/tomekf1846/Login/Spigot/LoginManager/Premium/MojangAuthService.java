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

public class MojangAuthService {

    public static MojangProfile queryHasJoined(String username, String serverHash) {
        try {
            for (int attempt = 0; attempt < 6; attempt++) {
                String urlStr = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                        + java.net.URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&serverId=" + java.net.URLEncoder.encode(serverHash, StandardCharsets.UTF_8);

                HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
                con.setConnectTimeout(7000);
                con.setReadTimeout(7000);
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "tomekf1846-Login/1.1");

                int code = con.getResponseCode();
                if (code == 200) {
                    JsonObject obj = JsonParser.parseReader(
                            new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();

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
                } else if (code == 204) {
                    return null;
                } else {
                    System.out.println("[PremiumLogin] hasJoined HTTP code=" + code);
                    if (code == 403 | code == 429) {
                        Thread.sleep(200L);
                    }
                }
                Thread.sleep(120L);
            }
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

        BigInteger bi = new BigInteger(digest);
        String hex = bi.toString(16);

        int expectedLength = digest.length * 2;
        if (hex.startsWith("-")) {
            String body = hex.substring(1);
            if (body.length() < expectedLength) {
                body = "0".repeat(expectedLength - body.length()) + body;
            }
            return "-" + body;
        }

        if (hex.length() < expectedLength) {
            hex = "0".repeat(expectedLength - hex.length()) + hex;
        }

        return hex;
    }
}
