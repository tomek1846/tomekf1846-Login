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

    /**
     * Przeszukuje wszystkie zapisane sesje i próbuje odszyfrować encToken przy ich pomocy.
     * Jeśli odszyfrowanie się uda i porównanie tokenów pasuje -> zwracamy tę sesję.
     */
    public PremiumSession findSessionByDecrypting(byte[] encToken) {
        for (Map.Entry<String, PremiumSession> e : sessions.entrySet()) {
            PremiumSession s = e.getValue();
            try {
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, s.keyPair.getPrivate());
                byte[] token = rsa.doFinal(encToken);
                if (java.util.Arrays.equals(token, s.verifyToken)) {
                    return s;
                }
            } catch (Throwable ignored) {
                // nie pasuje - kontynuuj
            }
        }
        return null;
    }

    /**
     * Usuń daną sesję spod wszystkich kluczy w mapie sessions (cleanup).
     * (Zachowuje dokładnie to samo zachowanie co oryginał).
     */
    public void removeSession(PremiumSession session) {
        if (session == null) return;
        Iterator<Map.Entry<String, PremiumSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PremiumSession> e = it.next();
            if (e.getValue() == session) {
                it.remove();
            }
        }
        Iterator<Map.Entry<String, MojangProfile>> itv = verifiedProfiles.entrySet().iterator();
        while (itv.hasNext()) {
            Map.Entry<String, MojangProfile> e = itv.next();
            break;
        }
    }

    /**
     * Zwraca przykładowy (jeden) klucz mapy sessions pod którym przechowywana była dana sesja.
     */
    public String getAnyKeyForSession(PremiumSession session) {
        for (Map.Entry<String, PremiumSession> e : sessions.entrySet()) {
            if (e.getValue() == session) return e.getKey();
        }
        return null;
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
