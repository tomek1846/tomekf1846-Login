package pl.tomekf1846.Login.Spigot.LoginManager.Other;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlayerRestrictions implements Listener {

    private static final Set<Player> blockedPlayers = new HashSet<>();
    private static List<String> allowedCommands = new ArrayList<>();

    public PlayerRestrictions() {
        loadAllowedCommands();
    }

    public static void reloadAllowedCommands() {
        allowedCommands.clear();
        PlayerRestrictions.loadAllowedCommands();
    }

    private static void loadAllowedCommands() {
        FileConfiguration config = MainSpigot.getInstance().getConfig();
        allowedCommands = config.getStringList("Main-Settings.Login-Command");
    }

    public static boolean isPlayerBlocked(Player player) {
        return blockedPlayers.contains(player);
    }

    public static void blockPlayer(Player player) {
        if (SessionCrackedManager.SessionCheck(player)) {
            LoginMessagesManager.CrackedSessionLoginTitle(player);
        } else {
            blockedPlayers.add(player);
            player.setInvulnerable(true);
            LoginMessagesManager.MessagesStart(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, true, false));
        }
    }

    public static void unblockPlayer(Player player) {
        blockedPlayers.remove(player);
        PlayerDataSave.savePlayerIPHistory(player);
        player.setInvulnerable(false);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        LoginMessagesManager.hideInfo(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.blocked-chat"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().split(" ")[0];

        if (allowedCommands.contains(command.replace("/", ""))) {
            return;
        }

        if (isPlayerBlocked(player)) {
            event.setCancelled(true);
            player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.blocked-command"));
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isPlayerBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isPlayerBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemMend(PlayerItemMendEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isPlayerBlocked(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (isPlayerBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isPlayerBlocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isPlayerBlocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player player && isPlayerBlocked(player)) {
            event.setResult(null);
        }
    }

    private String getPrefix(Player player) {
        return LanguageManager.getMessage(player, "messages.prefix.main-prefix");
    }
}
