package pl.tomekf1846.Login.Spigot.GUI.PlayerList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other.PlayerListPageManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other.PlayerListStateManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search.PlayerListSearch;

public class PlayerListGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String guiTitle = LanguageManager.getMessage("messages.gui.Playerlist.name");
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Material clickedMaterial = clickedItem.getType();
            ItemMeta itemMeta = clickedItem.getItemMeta();
            String clickedName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "";

            Player player = (Player) event.getWhoClicked();

            if (clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Close.material")) &&
                    clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Close.name"))) {
                player.closeInventory();
            } else if ((clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Onlinedye.material")) &&
                    clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Onlinedye.name"))) ||
                    (clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Offlinedye.material")) &&
                            clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Offlinedye.name")))) {
                PlayerListStateManager.toggleOnlineOffline(player);
            } else if (clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Search-player.material")) &&
                    clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Search-player.name"))) {
                PlayerListSearch.startSearch(player);
                player.closeInventory();
            } else if (clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Next-page.material")) &&
                    clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Next-page.name"))) {
                PlayerListPageManager.nextPage(player);
            } else if (clickedMaterial == Material.valueOf(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Previous-page.material")) &&
                    clickedName.equals(LanguageManager.getMessage("messages.gui.Playerlist.buttons.Previous-page.name"))) {
                PlayerListPageManager.previousPage(player);
            }
        }
    }
}
