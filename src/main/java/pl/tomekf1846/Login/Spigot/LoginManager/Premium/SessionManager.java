package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import javax.crypto.Cipher;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, PremiumSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, MojangProfile> verifiedProfiles = new ConcurrentHashMap<>();

    public void put(String key, PremiumSession session) {
        if (key == null || session == null) return;
        sessions.put(key, session);
    }

    public PremiumSession get(String key) {
        if (key == null) return null;
        return sessions.get(key);
    }

    public PremiumSession findSessionByDecrypting(byte[] encToken) {
        for (PremiumSession s : sessions.values()) {
            try {
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, s.keyPair.getPrivate());
                byte[] token = rsa.doFinal(encToken);
                if (Arrays.equals(token, s.verifyToken)) {
                    return s;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public void removeSession(PremiumSession session) {
        if (session == null) return;
        sessions.entrySet().removeIf(e -> e.getValue() == session);
    }

    public MojangProfile consumeVerifiedProfile(String connKey) {
        return verifiedProfiles.remove(connKey);
    }

    public void storeVerifiedProfile(String key, MojangProfile profile) {
        if (key == null || profile == null) return;
        verifiedProfiles.put(key, profile);
    }

    public void clearSessions() {
        sessions.clear();
        verifiedProfiles.clear();
    }
}
