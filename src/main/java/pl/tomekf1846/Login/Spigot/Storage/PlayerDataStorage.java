package pl.tomekf1846.Login.Spigot.Storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface PlayerDataStorage {
    void savePlayerData(OfflinePlayer player, String password);

    void savePlayerIPHistory(Player player);

    Map<String, String> loadPlayerData(UUID uuid);

    Map<UUID, Map<String, String>> loadAllPlayerData();

    void savePlayerLeaveTime(Player player);

    void saveLoginAttempt(LoginAttemptRecord attempt);

    void setPlayerPassword(UUID uuid, String newPassword);

    void setPlayerEmail(UUID uuid, String newEmail);

    void setPlayerLanguage(UUID uuid, String language);

    String getPlayerLanguage(UUID uuid);

    boolean setPlayerSession(String nick, boolean isPremium);

    UUID findUUIDByNick(String nick);

    boolean deletePlayerData(UUID uuid);

    void close();
}
