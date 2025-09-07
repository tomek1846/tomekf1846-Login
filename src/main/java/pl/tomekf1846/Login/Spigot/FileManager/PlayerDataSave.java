package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlayerDataSave {

    private static final String DATA_FOLDER = "plugins/tomekf1846-Login/Data";

    public static void savePlayerData(OfflinePlayer player, String password) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String firstIP = (player.isOnline() && player.getPlayer() != null)
                ? player.getPlayer().getAddress().getAddress().getHostAddress()
                : "offline";

        File dataFolder = new File(DATA_FOLDER);
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Nick", playerName);
        config.set("Player-UUID", uuid.toString());
        config.set("Premium-UUID", "");
        config.set("FirstIP", firstIP);
        config.set("LastIP", "");
        config.set("Email", "none");
        config.set("Premium", "");
        config.set("Password", password);
        config.set("Version", 1);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void savePlayerIPHistory(Player player) {
        UUID uuid = player.getUniqueId();
        String currentIP = player.getAddress().getAddress().getHostAddress();
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());

        File playerFile = new File(DATA_FOLDER, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        List<String> ipHistory = config.getStringList("PlayerIP");
        ipHistory.add(timestamp + " - " + currentIP);
        config.set("PlayerIP", ipHistory);
        if (!config.contains("FirstIP") || config.getString("FirstIP").isEmpty()) {
            config.set("FirstIP", currentIP);
        }
        config.set("LastIP", currentIP);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> loadPlayerData(UUID uuid) {
        File playerFile = new File(DATA_FOLDER, uuid.toString() + ".yml");

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

        return playerData;
    }

    public static Map<UUID, Map<String, String>> loadAllPlayerData() {
        File dataFolder = new File(DATA_FOLDER);
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

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

    public static void savePlayerLeaveTime(Player player) {
        UUID uuid = player.getUniqueId();
        String leaveTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());

        File playerFile = new File(DATA_FOLDER, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("LeaveTime", leaveTime);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerPassword(UUID uuid, String newPassword) {
        if (uuid == null || newPassword == null || newPassword.isEmpty()) {
            return;
        }
        File playerFile = new File(DATA_FOLDER, uuid + ".yml");

        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Password", newPassword);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerEmail(UUID uuid, String newEmail) {
        if (uuid == null || newEmail == null || newEmail.isEmpty()) {
            return;
        }
        File playerFile = new File(DATA_FOLDER, uuid + ".yml");

        if (!playerFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("Email", newEmail);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPlayerPremium(String nick) {
        UUID uuid = NickUuidCheck.getUUIDFromNick(nick);
        if (uuid == null) {
            return false;
        }
        Map<String, String> playerData = PlayerDataSave.loadPlayerData(uuid);
        if (playerData == null) {
            return false;
        }
        String premiumStatus = playerData.get("Premium");
        if (premiumStatus != null && premiumStatus.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean setPlayerSession(String nick, boolean isPremium) {
        UUID uuid;
        Player player = Bukkit.getPlayerExact(nick);
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(nick);
            uuid = offlinePlayer.getUniqueId();
        }
        if (uuid == null) {
            return false;
        }
        File dataFolder = new File(DATA_FOLDER);
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        playerConfig.set("Premium", isPremium);
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}


