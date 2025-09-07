package pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked;

import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

import java.util.*;

public class SessionCrackedManager {
    public static final Map<UUID, Integer> playerLoginCountCache = new HashMap<>();

    public static boolean SessionCheck(Player player) {
        UUID uuid = player.getUniqueId();
        String currentIP = player.getAddress().getAddress().getHostAddress();
        Map<String, String> playerData = PlayerDataSave.loadPlayerData(uuid);

        if (playerData == null) {
            return false;
        }
        int loginCount = playerLoginCountCache.getOrDefault(uuid, 0);
        if (loginCount >= 3) {
            return true;
        }
        String lastIP = playerData.get("LastIP");
        if (lastIP != null && lastIP.equals(currentIP)) {
            String leaveTimeStr = playerData.get("LeaveTime");
            if (leaveTimeStr != null) {
                long leaveTimeMillis = parseLeaveTime(leaveTimeStr);
                if (leaveTimeMillis != -1) {
                    long currentTime = System.currentTimeMillis();
                    long timeDifference = currentTime - leaveTimeMillis;
                    return timeDifference < 420000;
                }
            }
        }
        return false;
    }

    private static long parseLeaveTime(String leaveTimeStr) {
        try {
            return Long.parseLong(leaveTimeStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    public static void incrementLoginCount(UUID uuid) {
        int currentCount = playerLoginCountCache.getOrDefault(uuid, 0);
        playerLoginCountCache.put(uuid, currentCount + 1);
    }
    public static void clearLoginCounts() {
        playerLoginCountCache.clear();
    }

    public static void clearLoginCount(UUID uuid) {
        playerLoginCountCache.remove(uuid);
    }
}