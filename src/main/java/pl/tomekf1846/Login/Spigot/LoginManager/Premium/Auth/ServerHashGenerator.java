package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;

public final class ServerHashGenerator {

    private ServerHashGenerator() {
    }

    public static String compute(String serverId, byte[] sharedSecret, PublicKey publicKey) throws Exception {
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