package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public class PremiumSession {
    public final String username;
    public final String serverId;
    public final byte[] verifyToken;
    public final KeyPair keyPair;

    // set after EncryptionResponse
    public SecretKey sharedKey;

    public PremiumSession(String username, String serverId, byte[] verifyToken, KeyPair keyPair) {
        this.username = username;
        this.serverId = serverId;
        this.verifyToken = verifyToken;
        this.keyPair = keyPair;
    }
}