package pl.tomekf1846.Login.Spigot.Security;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Consumer;

public class PasswordSecurity {
    private static final String PREFIX_SEPARATOR = ":";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int AES_GCM_IV_LENGTH = 12;
    private static final int AES_CBC_IV_LENGTH = 16;
    private static final int CHACHA20_NONCE_LENGTH = 12;

    private PasswordSecurity() {
    }

    public static void encodeAsync(JavaPlugin plugin, String password, Consumer<String> callback) {
        if (plugin == null) {
            plugin = MainSpigot.getInstance();
        }
        JavaPlugin finalPlugin = plugin;
        Bukkit.getScheduler().runTaskAsynchronously(finalPlugin, () -> {
            String encoded = encode(password);
            Bukkit.getScheduler().runTask(finalPlugin, () -> callback.accept(encoded));
        });
    }

    public static String encode(String password) {
        if (password == null) {
            return null;
        }
        PasswordStorageMode mode = SecuritySettings.getPasswordStorageMode();
        if (mode == PasswordStorageMode.NONE) {
            return password;
        }
        try {
            return encodeWithMode(mode, password);
        } catch (GeneralSecurityException ex) {
            MainSpigot.getInstance().getLogger().warning("Failed to encode password securely: " + ex.getMessage());
            return password;
        }
    }

    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        String decoded = decode(stored);
        if (decoded == null) {
            return stored.equals(rawPassword);
        }
        return decoded.equals(rawPassword);
    }

    public static String decode(String stored) {
        if (stored == null) {
            return null;
        }
        ParsedStored parsed = parseStored(stored);
        if (parsed.mode == PasswordStorageMode.NONE) {
            return stored;
        }
        try {
            return decodeWithMode(parsed.mode, parsed.payload);
        } catch (GeneralSecurityException ex) {
            MainSpigot.getInstance().getLogger().warning("Failed to decode password securely: " + ex.getMessage());
            return null;
        }
    }

    public static String formatForDisplay(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        PasswordDisplayMode displayMode = SecuritySettings.getPasswordDisplayMode();
        if (displayMode == PasswordDisplayMode.HIDDEN) {
            return SecuritySettings.getHiddenPasswordText();
        }
        String decoded = decode(stored);
        if (decoded == null) {
            decoded = stored;
        }
        if (displayMode == PasswordDisplayMode.PLAIN) {
            return decoded;
        }
        return "*".repeat(decoded.length());
    }

    private static String encodeWithMode(PasswordStorageMode mode, String password) throws GeneralSecurityException {
        return switch (mode) {
            case AES_GCM -> encodeAesGcm(password);
            case AES_CBC -> encodeAesCbc(password);
            case CHACHA20 -> encodeChaCha20(password);
            case NONE -> password;
        };
    }

    private static String decodeWithMode(PasswordStorageMode mode, String payload) throws GeneralSecurityException {
        return switch (mode) {
            case AES_GCM -> decodeAesGcm(payload);
            case AES_CBC -> decodeAesCbc(payload);
            case CHACHA20 -> decodeChaCha20(payload);
            case NONE -> payload;
        };
    }

    private static String encodeAesGcm(String password) throws GeneralSecurityException {
        byte[] iv = new byte[AES_GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, buildAesKey(), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return buildPayload(PasswordStorageMode.AES_GCM, iv, encrypted);
    }

    private static String decodeAesGcm(String payload) throws GeneralSecurityException {
        PayloadParts parts = splitPayload(payload);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, buildAesKey(), new GCMParameterSpec(128, parts.iv));
        byte[] decrypted = cipher.doFinal(parts.cipherText);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String encodeAesCbc(String password) throws GeneralSecurityException {
        byte[] iv = new byte[AES_CBC_IV_LENGTH];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, buildAesKey(), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return buildPayload(PasswordStorageMode.AES_CBC, iv, encrypted);
    }

    private static String decodeAesCbc(String payload) throws GeneralSecurityException {
        PayloadParts parts = splitPayload(payload);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, buildAesKey(), new IvParameterSpec(parts.iv));
        byte[] decrypted = cipher.doFinal(parts.cipherText);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String encodeChaCha20(String password) throws GeneralSecurityException {
        byte[] nonce = new byte[CHACHA20_NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        SecretKey key = buildChaChaKey();
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return buildPayload(PasswordStorageMode.CHACHA20, nonce, encrypted);
    }

    private static String decodeChaCha20(String payload) throws GeneralSecurityException {
        PayloadParts parts = splitPayload(payload);
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        SecretKey key = buildChaChaKey();
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(parts.iv));
        byte[] decrypted = cipher.doFinal(parts.cipherText);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String buildPayload(PasswordStorageMode mode, byte[] iv, byte[] cipherText) {
        String encodedIv = Base64.getEncoder().encodeToString(iv);
        String encodedCipher = Base64.getEncoder().encodeToString(cipherText);
        return mode.name() + PREFIX_SEPARATOR + encodedIv + PREFIX_SEPARATOR + encodedCipher;
    }

    private static PayloadParts splitPayload(String payload) {
        String[] parts = payload.split(PREFIX_SEPARATOR, 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid encrypted password format.");
        }
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] cipherText = Base64.getDecoder().decode(parts[2]);
        return new PayloadParts(iv, cipherText);
    }

    private static ParsedStored parseStored(String stored) {
        int separatorIndex = stored.indexOf(PREFIX_SEPARATOR);
        if (separatorIndex > 0) {
            String prefix = stored.substring(0, separatorIndex).toUpperCase(Locale.ROOT);
            for (PasswordStorageMode mode : PasswordStorageMode.values()) {
                if (mode != PasswordStorageMode.NONE && mode.name().equals(prefix)) {
                    return new ParsedStored(mode, stored);
                }
            }
        }
        return new ParsedStored(PasswordStorageMode.NONE, stored);
    }

    private static SecretKey buildAesKey() {
        byte[] keyBytes = normalizeSecret(32);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static SecretKey buildChaChaKey() {
        byte[] keyBytes = normalizeSecret(32);
        return new SecretKeySpec(keyBytes, "ChaCha20");
    }

    private static byte[] normalizeSecret(int length) {
        String secret = SecuritySettings.getPasswordSecret();
        if (secret == null || secret.isBlank()) {
            secret = "tomekf1846-login-default-secret";
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            decoded = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (decoded.length == length) {
            return decoded;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(decoded);
            if (hashed.length == length) {
                return hashed;
            }
            byte[] adjusted = new byte[length];
            System.arraycopy(hashed, 0, adjusted, 0, Math.min(length, hashed.length));
            return adjusted;
        } catch (GeneralSecurityException ex) {
            byte[] adjusted = new byte[length];
            System.arraycopy(decoded, 0, adjusted, 0, Math.min(length, decoded.length));
            return adjusted;
        }
    }

    private record PayloadParts(byte[] iv, byte[] cipherText) {
    }

    private record ParsedStored(PasswordStorageMode mode, String payload) {
    }
}
