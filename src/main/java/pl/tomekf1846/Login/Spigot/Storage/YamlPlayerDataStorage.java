package pl.tomekf1846.Login.Spigot.Storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class YamlPlayerDataStorage extends AbstractFilePlayerDataStorage {
    public YamlPlayerDataStorage(JavaPlugin plugin, File baseDirectory) {
        super(plugin, baseDirectory);
    }

    @Override
    public void savePlayerData(OfflinePlayer player, String password) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String firstIP = (player.isOnline() && player.getPlayer() != null)
                ? player.getPlayer().getAddress().getAddress().getHostAddress()
                : "offline";

        File playerFile = fileFor(uuid, ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Nick", playerName);
        config.set("Player-UUID", uuid.toString());
        config.set("Premium-UUID", "");
        config.set("FirstIP", firstIP);
        config.set("LastIP", "");
        config.set("Email", "none");
        config.set("Premium", "");
        config.set("Password", password);
        config.set("Language", "");
        config.set("Version", 1);
        if (!config.contains("PlayerIP")) {
            config.set("PlayerIP", new ArrayList<>());
        }

        saveConfigSafely(config, playerFile);
    }

    @Override
    public void savePlayerIPHistory(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        List<String> ipHistory = config.getStringList("PlayerIP");
        String currentIP = player.getAddress().getAddress().getHostAddress();
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        ipHistory.add(timestamp + " - " + currentIP);
        config.set("PlayerIP", ipHistory);
        if (!config.contains("FirstIP") || config.getString("FirstIP").isEmpty()) {
            config.set("FirstIP", currentIP);
        }
        config.set("LastIP", currentIP);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public Map<String, String> loadPlayerData(UUID uuid) {
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return null;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        Map<String, String> playerData = new HashMap<>();
        playerData.put("Nick", config.getString("Nick"));
        playerData.put("Player-UUID", config.getString("Player-UUID"));
        playerData.put("Premium-UUID", config.getString("Premium-UUID"));
        playerData.put("FirstIP", config.getString("FirstIP"));
        playerData.put("LastIP", config.getString("LastIP"));
        playerData.put("LeaveTime", config.getString("LeaveTime"));
        playerData.put("Email", config.getString("Email"));
        playerData.put("Premium", config.getString("Premium"));
        playerData.put("Password", config.getString("Password"));
        playerData.put("Language", config.getString("Language"));
        return playerData;
    }

    @Override
    public Map<UUID, Map<String, String>> loadAllPlayerData() {
        File[] playerFiles = baseDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        Map<UUID, Map<String, String>> allPlayerData = new HashMap<>();
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                UUID playerUUID = UUID.fromString(playerFile.getName().replace(".yml", ""));
                Map<String, String> playerData = loadPlayerData(playerUUID);
                if (playerData != null) {
                    allPlayerData.put(playerUUID, playerData);
                }
            }
        }
        return allPlayerData;
    }

    @Override
    public void savePlayerLeaveTime(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        String leaveTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        config.set("LeaveTime", leaveTime);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public void saveLoginAttempt(LoginAttemptRecord attempt) {
        if (attempt == null) {
            return;
        }
        UUID uuid = attempt.getUuid();
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        List<Map<String, Object>> attempts = new ArrayList<>();
        List<?> existing = config.getList("LoginAttempts");
        if (existing != null) {
            for (Object entry : existing) {
                if (entry instanceof Map) {
                    attempts.add(new LinkedHashMap<>((Map<String, Object>) entry));
                }
            }
        }
        attempts.add(attempt.toMap());
        config.set("LoginAttempts", attempts);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public void setPlayerPassword(UUID uuid, String newPassword) {
        if (uuid == null || newPassword == null || newPassword.isEmpty()) {
            return;
        }
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Password", newPassword);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public void setPlayerEmail(UUID uuid, String newEmail) {
        if (uuid == null || newEmail == null || newEmail.isEmpty()) {
            return;
        }
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Email", newEmail);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public void setPlayerLanguage(UUID uuid, String language) {
        if (uuid == null || language == null || language.isEmpty()) {
            return;
        }
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Language", language);
        saveConfigSafely(config, playerFile);
    }

    @Override
    public String getPlayerLanguage(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        File playerFile = fileFor(uuid, ".yml");
        if (!playerFile.exists()) {
            return null;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.getString("Language");
    }

    @Override
    public boolean setPlayerSession(String nick, boolean isPremium) {
        UUID uuid = findUUIDByNick(nick);
        if (uuid == null) {
            return false;
        }
        File playerFile = fileFor(uuid, ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Premium", isPremium);
        saveConfigSafely(config, playerFile);
        return true;
    }

    @Override
    public UUID findUUIDByNick(String nick) {
        if (nick == null || nick.isEmpty()) {
            return null;
        }
        File[] files = baseDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return null;
        }
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String foundNick = config.getString("Nick");
            if (foundNick != null && foundNick.equalsIgnoreCase(nick)) {
                String uuid = config.getString("Player-UUID");
                if (uuid != null) {
                    return UUID.fromString(uuid);
                }
            }
        }
        return null;
    }

    @Override
    public boolean deletePlayerData(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        File playerFile = fileFor(uuid, ".yml");
        return !playerFile.exists() || playerFile.delete();
    }

    private void saveConfigSafely(FileConfiguration config, File playerFile) {
        File tempFile = new File(playerFile.getParentFile(), playerFile.getName() + ".tmp");
        try {
            config.save(tempFile);
            safeReplace(tempFile.toPath(), playerFile.toPath());
        } catch (IOException ex) {
            logSaveFailure(playerFile, ex);
            if (tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
}
