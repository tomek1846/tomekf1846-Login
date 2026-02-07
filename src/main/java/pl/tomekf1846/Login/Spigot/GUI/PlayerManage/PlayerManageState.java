package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManageState {
    public enum InputMode {
        EMAIL,
        PASSWORD
    }

    public static final NamespacedKey TARGET_PLAYER_KEY = new NamespacedKey(MainSpigot.getInstance(), "player_manage_target");

    private static final Map<UUID, String> targetPlayers = new HashMap<>();
    private static final Map<UUID, InputMode> inputModes = new HashMap<>();
    private static final Map<UUID, BukkitTask> titleTasks = new HashMap<>();

    public static void setTarget(Player viewer, String targetName) {
        targetPlayers.put(viewer.getUniqueId(), targetName);
    }

    public static String getTarget(Player viewer) {
        return targetPlayers.get(viewer.getUniqueId());
    }

    public static void clearTarget(Player viewer) {
        targetPlayers.remove(viewer.getUniqueId());
    }

    public static void startInput(Player viewer, String targetName, InputMode mode) {
        setTarget(viewer, targetName);
        inputModes.put(viewer.getUniqueId(), mode);
        sendInputTitle(viewer, mode);
    }

    public static InputMode getInputMode(Player viewer) {
        return inputModes.get(viewer.getUniqueId());
    }

    public static void clearInput(Player viewer) {
        inputModes.remove(viewer.getUniqueId());
        BukkitTask task = titleTasks.remove(viewer.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public static boolean cancelInput(Player viewer) {
        if (!inputModes.containsKey(viewer.getUniqueId())) {
            return false;
        }
        clearInput(viewer);
        String targetName = getTarget(viewer);
        if (targetName != null && !targetName.isBlank()) {
            PlayerManageGui.openGUI(viewer, targetName);
        }
        return true;
    }

    private static void sendInputTitle(Player viewer, InputMode mode) {
        BukkitTask existing = titleTasks.remove(viewer.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(MainSpigot.getInstance(), () -> {
            if (!inputModes.containsKey(viewer.getUniqueId())) {
                BukkitTask running = titleTasks.remove(viewer.getUniqueId());
                if (running != null) {
                    running.cancel();
                }
                return;
            }
            String baseKey = mode == InputMode.EMAIL
                    ? "messages.gui.PlayerManage.input.email"
                    : "messages.gui.PlayerManage.input.password";
            viewer.sendTitle(
                    LanguageManager.getMessage(viewer, baseKey + ".title"),
                    LanguageManager.getMessage(viewer, baseKey + ".subtitle"),
                    10, 40, 10
            );
        }, 0L, 40L);
        titleTasks.put(viewer.getUniqueId(), task);
    }
}
