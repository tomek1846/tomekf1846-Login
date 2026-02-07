package pl.tomekf1846.Login.Spigot.GUI.PlayerList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other.PlayerListPageManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other.PlayerListStateManager;

import java.util.List;

public class PlayerListGui {

    private static final PlayerListStateManager stateManager = new PlayerListStateManager();

    public static void openGUI(Player player, int page) {
        String title = LanguageManager.getMessage(player, "messages.gui.Playerlist.name");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        String layout = LanguageManager.getMessage(player, "messages.gui.Playerlist.layout");

        ItemStack a = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.filling.filling-A")),
                "§7",
                List.of()
        );

        ItemStack b = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.filling.filling-B")),
                "§7",
                List.of()
        );

        ItemStack c = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.filling.filling-C")),
                "§7",
                List.of()
        );

        ItemStack sign = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Search-player.material")),
                LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Search-player.name"),
                LanguageManager.getMessageList(player, "messages.gui.Playerlist.buttons.Search-player.lore"));

        ItemStack arrow1 = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Previous-page.material")),
                LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Previous-page.name"),
                LanguageManager.getMessageList(player, "messages.gui.Playerlist.buttons.Previous-page.lore").stream()
                        .map(lore -> lore.replace("{page}", String.valueOf(page)))
                        .toList());

        ItemStack arrow2 = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Next-page.material")),
                LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Next-page.name"),
                LanguageManager.getMessageList(player, "messages.gui.Playerlist.buttons.Next-page.lore").stream()
                        .map(lore -> lore.replace("{page}", String.valueOf(page)))
                        .toList());

        ItemStack barrier = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Close.material")),
                LanguageManager.getMessage(player, "messages.gui.Playerlist.buttons.Close.name"),
                LanguageManager.getMessageList(player, "messages.gui.Playerlist.buttons.Close.lore"));

        ItemStack onlineOfflineDye = stateManager.getOnlineItem(player);

        for (int i = 0; i < layout.length(); i++) {
            switch (layout.charAt(i)) {
                case 'A':
                    gui.setItem(i, a);
                    break;
                case 'B':
                    gui.setItem(i, b);
                    break;
                case 'C':
                    gui.setItem(i, c);
                    break;
                case '1':
                    gui.setItem(i, sign);
                    break;
                case '2':
                    gui.setItem(i, arrow1);
                    break;
                case '3':
                    gui.setItem(i, barrier);
                    break;
                case '4':
                    gui.setItem(i, arrow2);
                    break;
                case '5':
                case '6':
                    gui.setItem(i, onlineOfflineDye);
                    break;
                default:
                    break;
            }
        }

        int[] playerSlots = {
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35,
                36, 37, 38, 39, 40, 41, 42, 43, 44
        };

        ItemStack[] playerHeads = PlayerListPageManager.getPlayersForPage(player, page);
        for (int i = 0; i < playerHeads.length && i < playerSlots.length; i++) {
            gui.setItem(playerSlots[i], playerHeads[i]);
        }

        player.openInventory(gui);
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openFirstPage(Player player) {
        PlayerListPageManager.playerPages.put(player.getUniqueId(), 1);
        openGUI(player, 1);
    }
}
