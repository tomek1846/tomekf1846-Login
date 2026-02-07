package pl.tomekf1846.Login.Spigot.FileManager;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tomekf1846.Login.Spigot.Storage.PlayerDataStorage;
import pl.tomekf1846.Login.Spigot.Storage.StorageFactory;

import java.util.Map;
import java.util.UUID;

public class PlayerDataSave {

    private static PlayerDataStorage storage;

    public static void initialize(JavaPlugin plugin) {
        storage = StorageFactory.create(plugin);
    }

    public static void shutdown() {
        if (storage != null) {
            storage.close();
        }
    }

    public static void savePlayerData(OfflinePlayer player, String password) {
        ensureStorage();
        storage.savePlayerData(player, password);
    }

    public static void savePlayerIPHistory(Player player) {
        ensureStorage();
        storage.savePlayerIPHistory(player);
    }

    public static Map<String, String> loadPlayerData(UUID uuid) {
        ensureStorage();
        return storage.loadPlayerData(uuid);
    }

    public static Map<UUID, Map<String, String>> loadAllPlayerData() {
        ensureStorage();
        return storage.loadAllPlayerData();
    }

    public static void savePlayerLeaveTime(Player player) {
        ensureStorage();
        storage.savePlayerLeaveTime(player);
    }

    public static void setPlayerPassword(UUID uuid, String newPassword) {
        ensureStorage();
        storage.setPlayerPassword(uuid, newPassword);
    }

    public static void setPlayerEmail(UUID uuid, String newEmail) {
        ensureStorage();
        storage.setPlayerEmail(uuid, newEmail);
    }

    public static boolean isPlayerPremium(String nick) {
        UUID uuid = findUUIDByNick(nick);
        if (uuid == null) {
            return false;
        }
        Map<String, String> playerData = loadPlayerData(uuid);
        if (playerData == null) {
            return false;
        }
        String premiumStatus = playerData.get("Premium");
        return premiumStatus != null && premiumStatus.equalsIgnoreCase("true");
    }

    public static boolean setPlayerSession(String nick, boolean isPremium) {
        ensureStorage();
        return storage.setPlayerSession(nick, isPremium);
    }

    public static UUID findUUIDByNick(String nick) {
        ensureStorage();
        return storage.findUUIDByNick(nick);
    }

    public static boolean deletePlayerData(UUID uuid) {
        ensureStorage();
        return storage.deletePlayerData(uuid);
    }

    private static void ensureStorage() {
        if (storage == null) {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PlayerDataSave.class);
            storage = StorageFactory.create(plugin);
        }
    }
}
