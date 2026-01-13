package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session;

import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumVerifiedProfileStore {

    private static final Duration TTL = Duration.ofSeconds(45);

    private final ConcurrentHashMap<String, Entry> verifiedProfiles = new ConcurrentHashMap<>();

    public void store(String username, String ip, MojangProfile profile) {
        if (profile == null || username == null || username.isBlank()) {
            return;
        }
        String key = normalize(username);
        verifiedProfiles.put(key, new Entry(profile, normalizeIp(ip), Instant.now()));
    }

    public MojangProfile consume(String username, String ip) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String key = normalize(username);
        Entry entry = verifiedProfiles.get(key);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            verifiedProfiles.remove(key, entry);
            return null;
        }
        String normalizedIp = normalizeIp(ip);
        if (entry.ip != null && normalizedIp != null && !Objects.equals(entry.ip, normalizedIp)) {
            return null;
        }
        verifiedProfiles.remove(key, entry);
        return entry.profile;
    }

    public void discard(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String key = normalize(username);
        verifiedProfiles.remove(key);
    }

    public void clear() {
        verifiedProfiles.clear();
    }

    private boolean isExpired(Entry entry) {
        return entry != null && entry.createdAt.plus(TTL).isBefore(Instant.now());
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String ip) {
        return (ip == null || ip.isBlank()) ? null : ip.trim();
    }

    private record Entry(MojangProfile profile, String ip, Instant createdAt) {
    }
}
