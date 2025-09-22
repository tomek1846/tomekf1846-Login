package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session;

import javax.crypto.Cipher;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class PremiumSessionRegistry {

    private static final long SESSION_EXPIRY_MILLIS = 30_000L;

    private final ConcurrentMap<String, ConcurrentLinkedQueue<PremiumSession>> pendingSessions = new ConcurrentHashMap<>();

    public void register(PremiumSession session) {
        if (session == null || session.username == null || session.username.isBlank()) {
            return;
        }
        String key = session.username.toLowerCase(Locale.ROOT);
        pendingSessions.compute(key, (k, queue) -> {
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }
            queue.add(session);
            return queue;
        });
    }

    public PremiumSession claim(String username, byte[] encryptedToken) {
        if (encryptedToken == null) {
            return null;
        }

        if (username != null && !username.isBlank()) {
            String key = username.toLowerCase(Locale.ROOT);
            ConcurrentLinkedQueue<PremiumSession> queue = pendingSessions.get(key);
            PremiumSession session = pollQueue(key, queue, encryptedToken);
            if (session != null) {
                return session;
            }
        }

        for (Map.Entry<String, ConcurrentLinkedQueue<PremiumSession>> entry : pendingSessions.entrySet()) {
            PremiumSession session = pollQueue(entry.getKey(), entry.getValue(), encryptedToken);
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    private PremiumSession pollQueue(String key, Queue<PremiumSession> queue, byte[] encryptedToken) {
        if (queue == null) {
            return null;
        }

        PremiumSession match = null;
        long now = System.currentTimeMillis();
        for (Iterator<PremiumSession> it = queue.iterator(); it.hasNext(); ) {
            PremiumSession candidate = it.next();
            if (matchesVerifyToken(candidate, encryptedToken)) {
                match = candidate;
                it.remove();
                break;
            }

            if (now - candidate.createdAt > SESSION_EXPIRY_MILLIS) {
                it.remove();
            }
        }

        if (queue.isEmpty()) {
            pendingSessions.remove(key, queue);
        }

        return match;
    }

    private boolean matchesVerifyToken(PremiumSession session, byte[] encryptedToken) {
        if (session == null || encryptedToken == null) {
            return false;
        }
        try {
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
            byte[] token = rsa.doFinal(encryptedToken);
            return Arrays.equals(token, session.verifyToken);
        } catch (Exception ignored) {
        }
        return false;
    }

    public void remove(PremiumSession session) {
        if (session == null || session.username == null || session.username.isBlank()) {
            return;
        }
        String key = session.username.toLowerCase(Locale.ROOT);
        Queue<PremiumSession> queue = pendingSessions.get(key);
        if (queue == null) {
            return;
        }
        queue.remove(session);
        if (queue.isEmpty()) {
            pendingSessions.remove(key, queue);
        }
    }

    public void discard(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String key = username.toLowerCase(Locale.ROOT);
        Queue<PremiumSession> queue = pendingSessions.remove(key);
        if (queue != null) {
            queue.clear();
        }
    }

    public void clear() {
        pendingSessions.clear();
    }
}