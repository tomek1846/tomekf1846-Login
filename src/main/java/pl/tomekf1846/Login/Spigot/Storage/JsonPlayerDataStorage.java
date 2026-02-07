package pl.tomekf1846.Login.Spigot.Storage;

import com.google.gson.JsonSyntaxException;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class JsonPlayerDataStorage extends AbstractFilePlayerDataStorage {
    public JsonPlayerDataStorage(JavaPlugin plugin, File baseDirectory) {
        super(plugin, baseDirectory);
    }

    @Override
    public void savePlayerData(OfflinePlayer player, String password) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String firstIP = (player.isOnline() && player.getPlayer() != null)
                ? player.getPlayer().getAddress().getAddress().getHostAddress()
                : "offline";
        PlayerRecord record = PlayerRecord.fromDefaults(uuid, playerName, firstIP, password);
        writeRecord(uuid, record);
    }

    @Override
    public void savePlayerIPHistory(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            record = PlayerRecord.fromDefaults(uuid, player.getName(), "offline", "none");
        }
        String currentIP = player.getAddress().getAddress().getHostAddress();
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        List<String> history = record.getPlayerIP();
        history.add(timestamp + " - " + currentIP);
        if (record.getFirstIp() == null || record.getFirstIp().isEmpty()) {
            record.setFirstIp(currentIP);
        }
        record.setLastIp(currentIP);
        writeRecord(uuid, record);
    }

    @Override
    public Map<String, String> loadPlayerData(UUID uuid) {
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return null;
        }
        return record.toMap();
    }

    @Override
    public Map<UUID, Map<String, String>> loadAllPlayerData() {
        File[] playerFiles = baseDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        Map<UUID, Map<String, String>> allPlayerData = new HashMap<>();
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                String filename = playerFile.getName().replace(".json", "");
                try {
                    UUID uuid = UUID.fromString(filename);
                    PlayerRecord record = readRecord(uuid);
                    if (record != null) {
                        allPlayerData.put(uuid, record.toMap());
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip invalid
                }
            }
        }
        return allPlayerData;
    }

    @Override
    public void savePlayerLeaveTime(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return;
        }
        record.setLeaveTime(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
        writeRecord(uuid, record);
    }

    @Override
    public void saveLoginAttempt(LoginAttemptRecord attempt) {
        if (attempt == null) {
            return;
        }
        UUID uuid = attempt.getUuid();
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return;
        }
        record.getLoginAttempts().add(attempt);
        writeRecord(uuid, record);
    }

    @Override
    public void setPlayerPassword(UUID uuid, String newPassword) {
        if (uuid == null || newPassword == null || newPassword.isEmpty()) {
            return;
        }
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return;
        }
        record.setPassword(newPassword);
        writeRecord(uuid, record);
    }

    @Override
    public void setPlayerEmail(UUID uuid, String newEmail) {
        if (uuid == null || newEmail == null || newEmail.isEmpty()) {
            return;
        }
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return;
        }
        record.setEmail(newEmail);
        writeRecord(uuid, record);
    }

    @Override
    public boolean setPlayerSession(String nick, boolean isPremium) {
        UUID uuid = findUUIDByNick(nick);
        if (uuid == null) {
            return false;
        }
        PlayerRecord record = readRecord(uuid);
        if (record == null) {
            return false;
        }
        record.setPremium(String.valueOf(isPremium));
        writeRecord(uuid, record);
        return true;
    }

    @Override
    public UUID findUUIDByNick(String nick) {
        if (nick == null || nick.isEmpty()) {
            return null;
        }
        File[] playerFiles = baseDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (playerFiles == null) {
            return null;
        }
        for (File file : playerFiles) {
            String filename = file.getName().replace(".json", "");
            try {
                UUID uuid = UUID.fromString(filename);
                PlayerRecord record = readRecord(uuid);
                if (record != null && record.getNick() != null && record.getNick().equalsIgnoreCase(nick)) {
                    return uuid;
                }
            } catch (IllegalArgumentException ignored) {
                // skip invalid
            }
        }
        return null;
    }

    @Override
    public boolean deletePlayerData(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        File playerFile = fileFor(uuid, ".json");
        return !playerFile.exists() || playerFile.delete();
    }

    private PlayerRecord readRecord(UUID uuid) {
        File playerFile = fileFor(uuid, ".json");
        if (!playerFile.exists()) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(playerFile.toPath())) {
            return gson.fromJson(reader, PlayerRecord.class);
        } catch (IOException | JsonSyntaxException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed reading JSON player data: " + playerFile.getAbsolutePath(), ex);
            return null;
        }
    }

    private void writeRecord(UUID uuid, PlayerRecord record) {
        File playerFile = fileFor(uuid, ".json");
        File tempFile = new File(playerFile.getParentFile(), playerFile.getName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
            gson.toJson(record, writer);
            safeReplace(tempFile.toPath(), playerFile.toPath());
        } catch (IOException ex) {
            logSaveFailure(playerFile, ex);
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    plugin.getLogger().warning("Unable to delete temp file: " + tempFile.getAbsolutePath());
                }
            }
        }
    }
}
