package pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public class PlayerListSearchListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String searchTitle = LanguageManager.getMessage(player, "messages.gui.Playerlist.Search.name");

        if (event.getView().getTitle().equals(searchTitle)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            Material clicked = event.getCurrentItem().getType();

            if (clicked == Material.BARRIER) {
                player.closeInventory();
            }
        }
    }
}
