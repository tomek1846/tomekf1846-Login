package pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListSearch implements Listener {
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");

    public static Player searchingPlayer = null;

    public static void startSearch(Player player) {
        searchingPlayer = player;
        searchingPlayer.sendMessage(PREFIX + LanguageManager.getMessage("messages.gui.Playerlist.Search.search-message"));
        sendSearchTitle(player);
    }

    private static void sendSearchTitle(Player player) {
        Bukkit.getScheduler().runTaskTimer(MainSpigot.getInstance(), () -> {
            if (searchingPlayer != null && searchingPlayer.equals(player)) {
                player.sendTitle(LanguageManager.getMessage("messages.gui.Playerlist.Search.search-title.title"),
                        LanguageManager.getMessage("messages.gui.Playerlist.Search.search-title.subtitle"),
                        10, 40, 10);
            }
        }, 0L, 40L);
    }

    public static void searchAndDisplay(String searchText) {
        if (searchingPlayer == null) {
            return;
        }

        List<String> matchedPlayers = searchPlayers(searchText.toLowerCase());

        if (matchedPlayers.isEmpty()) {
            searchingPlayer.sendMessage(PREFIX + LanguageManager.getMessage("messages.gui.Playerlist.Search.no-player-data"));
        } else {
            Inventory gui = Bukkit.createInventory(null, 54, LanguageManager.getMessage("messages.gui.Playerlist.Search.name"));
            String layout = LanguageManager.getMessage("messages.gui.Playerlist.Search.layout");
            for (int i = 0; i < layout.length(); i++) {
                int slot = i;
                char ch = layout.charAt(i);
                switch (ch) {
                    case 'A':
                        gui.setItem(slot, new ItemStack(Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.filling.filling-A"))));
                        break;
                    case 'B':
                        gui.setItem(slot, new ItemStack(Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.filling.filling-B"))));
                        break;
                    case 'C':
                        gui.setItem(slot, new ItemStack(Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.filling.filling-C"))));
                        break;
                    case '1':
                        ItemStack barrier = new ItemStack(Material.BARRIER);
                        var meta = barrier.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(LanguageManager.getMessage("messages.gui.Playerlist.Search.buttons.Close.name"));
                            meta.setLore(LanguageManager.getMessageList("messages.gui.Playerlist.Search.buttons.Close.lore"));
                        }
                        barrier.setItemMeta(meta);
                        gui.setItem(slot, barrier);
                        break;
                    default:
                        break;
                }
            }

            int slotIndex = 9;
            for (String nick : matchedPlayers) {
                if (slotIndex >= 45) break;
                gui.setItem(slotIndex, createPlayerHead(nick));
                slotIndex++;
            }

            searchingPlayer.openInventory(gui);
        }

        searchingPlayer = null;
    }


    private static List<String> searchPlayers(String searchText) {
        List<String> matchedPlayers = new ArrayList<>();
        Map<UUID, Map<String, String>> allPlayerData = PlayerDataSave.loadAllPlayerData();

        for (Map.Entry<UUID, Map<String, String>> entry : allPlayerData.entrySet()) {
            String playerNick = entry.getValue().get("Nick");

            if (playerNick != null && playerNick.toLowerCase().contains(searchText)) {
                matchedPlayers.add(playerNick);
            }
        }
        return matchedPlayers;
    }

    private static ItemStack createPlayerHead(String nick) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage("messages.gui.Playerlist.Players.name").replace("{player}", nick));

            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(nick));
            } catch (Exception ignored) {
            }

            List<String> lore = new ArrayList<>();
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.uuid").replace("{uuid}", getPlayerData(nick, "Player-UUID")));
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.password").replace("{password}", getPlayerData(nick, "Password")));
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.fristip").replace("{fristip}", getPlayerData(nick, "FirstIP")));
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.lastip").replace("{lastip}", getPlayerData(nick, "LastIP")));
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.email").replace("{email}", getPlayerData(nick, "Email")));
            lore.add(LanguageManager.getMessage("messages.gui.Playerlist.Players.lore.premium").replace("{premium}", getPlayerData(nick, "Premium")));

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static String getPlayerData(String nick, String key) {
        Map<UUID, Map<String, String>> allPlayerData = PlayerDataSave.loadAllPlayerData();

        for (Map.Entry<UUID, Map<String, String>> entry : allPlayerData.entrySet()) {
            String playerNick = entry.getValue().get("Nick");
            if (playerNick != null && playerNick.equalsIgnoreCase(nick)) {
                String value = entry.getValue().get(key);
                if (value == null || value.isEmpty()) {
                    return LanguageManager.getMessage("messages.gui.no-data");
                }
                return value;
            }
        }
        return LanguageManager.getMessage("messages.gui.no-data");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        if (searchingPlayer != null && searchingPlayer.equals(player)) {
            event.setCancelled(true);

            Bukkit.getScheduler().runTask(MainSpigot.getInstance(), () -> searchAndDisplay(message));
        }
    }
}
