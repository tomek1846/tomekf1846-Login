package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

public class CryptoService {

    private final SecureRandom random = new SecureRandom();

    public PremiumSession createSession(String username) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();

        String serverId = "";
        byte[] verifyToken = new byte[16];
        random.nextBytes(verifyToken);

        return new PremiumSession(username, serverId, verifyToken, kp);
    }

    public static SecretKey sharedSecretToKey(byte[] sharedSecret) {
        return new SecretKeySpec(sharedSecret, "AES");
    }
}
