package pl.tomekf1846.Login.Spigot.FileManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tomekf1846.Login.Spigot.Storage.PlayerDataStorage;
import pl.tomekf1846.Login.Spigot.Storage.LoginAttemptRecord;
import pl.tomekf1846.Login.Spigot.Storage.StorageFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerDataSave {

    private static PlayerDataStorage storage;
    private static final Gson SNAPSHOT_GSON = new GsonBuilder().disableHtmlEscaping().create();

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

    public static void saveLoginAttempt(Player player, boolean success, String attemptedPassword, int wrongAttempts) {
        if (player == null) {
            return;
        }
        ensureStorage();
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        String ipAddress = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "unknown";
        String snapshotJson = SNAPSHOT_GSON.toJson(buildSnapshot(player));
        LoginAttemptRecord attempt = new LoginAttemptRecord(
                player.getUniqueId(),
                timestamp,
                success,
                attemptedPassword,
                wrongAttempts,
                ipAddress,
                snapshotJson
        );
        storage.saveLoginAttempt(attempt);
    }

    public static void setPlayerPassword(UUID uuid, String newPassword) {
        ensureStorage();
        storage.setPlayerPassword(uuid, newPassword);
    }

    public static void setPlayerEmail(UUID uuid, String newEmail) {
        ensureStorage();
        storage.setPlayerEmail(uuid, newEmail);
    }

    public static void setPlayerLanguage(UUID uuid, String language) {
        ensureStorage();
        storage.setPlayerLanguage(uuid, language);
    }

    public static String getPlayerLanguage(UUID uuid) {
        ensureStorage();
        return storage.getPlayerLanguage(uuid);
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

    private static Map<String, Object> buildSnapshot(Player player) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("player", buildPlayerInfo(player));
        snapshot.put("location", buildLocationInfo(player));
        snapshot.put("inventory", buildInventoryInfo(player));
        snapshot.put("enderChest", buildEnderChestInfo(player));
        snapshot.put("status", buildStatusInfo(player));
        snapshot.put("targetBlock", buildTargetBlockInfo(player));
        return snapshot;
    }

    private static Map<String, Object> buildPlayerInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", player.getName());
        info.put("uuid", player.getUniqueId().toString());
        info.put("gameMode", player.getGameMode().name());
        info.put("ipAddress", player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "unknown");
        return info;
    }

    private static Map<String, Object> buildLocationInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        Location location = player.getLocation();
        info.put("world", location.getWorld() != null ? location.getWorld().getName() : "unknown");
        info.put("x", location.getX());
        info.put("y", location.getY());
        info.put("z", location.getZ());
        info.put("yaw", location.getYaw());
        info.put("pitch", location.getPitch());
        Map<String, Object> direction = new LinkedHashMap<>();
        direction.put("x", location.getDirection().getX());
        direction.put("y", location.getDirection().getY());
        direction.put("z", location.getDirection().getZ());
        info.put("direction", direction);
        if (player.getRespawnLocation() != null) {
            Location respawn = player.getRespawnLocation();
            Map<String, Object> respawnInfo = new LinkedHashMap<>();
            respawnInfo.put("world", respawn.getWorld() != null ? respawn.getWorld().getName() : "unknown");
            respawnInfo.put("x", respawn.getX());
            respawnInfo.put("y", respawn.getY());
            respawnInfo.put("z", respawn.getZ());
            info.put("respawn", respawnInfo);
        }
        return info;
    }

    private static Map<String, Object> buildInventoryInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("items", serializeContents(player.getInventory().getContents()));
        info.put("armor", serializeContents(player.getInventory().getArmorContents()));
        info.put("extra", serializeContents(player.getInventory().getExtraContents()));
        return info;
    }

    private static List<Map<String, Object>> serializeContents(ItemStack[] contents) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (contents == null) {
            return items;
        }
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("slot", slot);
            entry.put("type", item.getType().name());
            entry.put("amount", item.getAmount());
            entry.put("data", item.serialize());
            items.add(entry);
        }
        return items;
    }

    private static Map<String, Object> buildEnderChestInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("items", serializeContents(player.getEnderChest().getContents()));
        return info;
    }

    private static Map<String, Object> buildStatusInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("health", player.getHealth());
        info.put("maxHealth", player.getMaxHealth());
        info.put("foodLevel", player.getFoodLevel());
        info.put("saturation", player.getSaturation());
        info.put("exhaustion", player.getExhaustion());
        Map<String, Object> exp = new LinkedHashMap<>();
        exp.put("level", player.getLevel());
        exp.put("exp", player.getExp());
        exp.put("totalExp", player.getTotalExperience());
        exp.put("expToLevel", player.getExpToLevel());
        info.put("experience", exp);
        Map<String, Object> movement = new LinkedHashMap<>();
        movement.put("walkSpeed", player.getWalkSpeed());
        movement.put("flySpeed", player.getFlySpeed());
        info.put("movement", movement);
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("isDead", player.isDead());
        flags.put("isOnline", player.isOnline());
        flags.put("isFlying", player.isFlying());
        flags.put("isSneaking", player.isSneaking());
        flags.put("isSprinting", player.isSprinting());
        flags.put("isSleeping", player.isSleeping());
        flags.put("isInsideVehicle", player.isInsideVehicle());
        flags.put("isOnGround", player.isOnGround());
        flags.put("allowFlight", player.getAllowFlight());
        flags.put("isGliding", player.isGliding());
        flags.put("isSwimming", player.isSwimming());
        flags.put("isOp", player.isOp());
        flags.put("isInvulnerable", player.isInvulnerable());
        flags.put("isInvisible", player.isInvisible());
        info.put("flags", flags);
        Map<String, Object> velocity = new LinkedHashMap<>();
        velocity.put("x", player.getVelocity().getX());
        velocity.put("y", player.getVelocity().getY());
        velocity.put("z", player.getVelocity().getZ());
        info.put("velocity", velocity);
        info.put("activeEffects", player.getActivePotionEffects().stream()
                .map(effect -> {
                    Map<String, Object> effectMap = new LinkedHashMap<>();
                    effectMap.put("type", effect.getType().getName());
                    effectMap.put("amplifier", effect.getAmplifier());
                    effectMap.put("duration", effect.getDuration());
                    effectMap.put("ambient", effect.isAmbient());
                    effectMap.put("particles", effect.hasParticles());
                    effectMap.put("icon", effect.hasIcon());
                    return effectMap;
                })
                .collect(Collectors.toList()));
        return info;
    }

    private static Map<String, Object> buildTargetBlockInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        Block target = player.getTargetBlockExact(8);
        if (target == null) {
            info.put("type", "none");
            return info;
        }
        info.put("type", target.getType().name());
        info.put("world", target.getWorld().getName());
        info.put("x", target.getX());
        info.put("y", target.getY());
        info.put("z", target.getZ());
        return info;
    }
}
