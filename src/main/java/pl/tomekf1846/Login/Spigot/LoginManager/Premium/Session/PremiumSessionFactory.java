package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

/**
 * Creates and prepares premium login sessions. Responsible for generating
 * a dedicated RSA key pair and a unique verification token for each premium
 * handshake handled by the plugin.
 */
public class PremiumSessionFactory {

    private final SecureRandom random = new SecureRandom();

    public PremiumSession createSession(String username) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String serverId = "";
        byte[] verifyToken = new byte[16];
        random.nextBytes(verifyToken);

        return new PremiumSession(username, serverId, verifyToken, keyPair);
    }

    public static SecretKey sharedSecretToKey(byte[] sharedSecret) {
        return new SecretKeySpec(sharedSecret, "AES");
    }
}
