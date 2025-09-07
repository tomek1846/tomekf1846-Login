package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoUtil {
    private CryptoUtil() {}

    public static SecretKey sharedSecretToKey(byte[] sharedSecret) {
        return new SecretKeySpec(sharedSecret, "AES");
    }
}
