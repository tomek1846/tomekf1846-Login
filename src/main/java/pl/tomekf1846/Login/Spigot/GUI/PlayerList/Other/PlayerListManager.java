package pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListManager {

    public static ItemStack[] getPlayerHeads(Player viewer) {
        Map<UUID, Map<String, String>> allPlayerData = PlayerDataSave.loadAllPlayerData();
        boolean showOnlineOnly = PlayerListStateManager.isOnline;

        java.util.List<ItemStack> playerHeads = new java.util.ArrayList<>();

        for (Map.Entry<UUID, Map<String, String>> entry : allPlayerData.entrySet()) {
            UUID playerUUID = entry.getKey();
            Player player = Bukkit.getPlayer(playerUUID);
            if (showOnlineOnly && (player == null || !player.isOnline())) {
                continue;
            }

            Map<String, String> playerData = entry.getValue();
            String nick = getOrDefault(viewer, playerData.get("Nick"));
            String uuid = getOrDefault(viewer, playerData.get("Player-UUID"));
            String password = getOrDefault(viewer, playerData.get("Password"));
            String firstIP = getOrDefault(viewer, playerData.get("FirstIP"));
            String lastIP = getOrDefault(viewer, playerData.get("LastIP"));
            String email = getOrDefault(viewer, playerData.get("Email"));
            String premium = getOrDefault(viewer, playerData.get("Premium"));

            playerHeads.add(createPlayerHead(viewer, nick, playerUUID, uuid, password, firstIP, lastIP, email, premium));
        }

        return playerHeads.toArray(new ItemStack[0]);
    }

    private static ItemStack createPlayerHead(Player viewer, String nick, UUID playerUUID, String uuid, String password, String firstIP, String lastIP, String email, String premium) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.name").replace("{player}", nick));

            List<String> lore = new ArrayList<>();
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.uuid").replace("{uuid}", getOrDefault(viewer, uuid)));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.password").replace("{password}", getOrDefault(viewer, password)));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.fristip").replace("{fristip}", getOrDefault(viewer, firstIP)));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.lastip").replace("{lastip}", getOrDefault(viewer, lastIP)));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.email").replace("{email}", getOrDefault(viewer, email)));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.Playerlist.Players.lore.premium").replace("{premium}", getOrDefault(viewer, premium)));

            meta.setLore(lore);

            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
            } catch (Exception ignored) {}

            head.setItemMeta(meta);
        }
        return head;
    }

    private static String getOrDefault(Player viewer, String value) {
        return (value == null || value.trim().isEmpty())
                ? LanguageManager.getMessage(viewer, "messages.gui.no-data")
                : value;
    }
}
