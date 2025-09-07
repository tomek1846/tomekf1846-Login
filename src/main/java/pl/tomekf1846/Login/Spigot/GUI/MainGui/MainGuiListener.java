package pl.tomekf1846.Login.Spigot.GUI.MainGui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.PlayerListGui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MainGuiListener implements Listener {

    private static final Map<String, Consumer<Player>> buttonActions = new HashMap<>();

    static {
        buttonActions.put("messages.gui.Maingui.buttons.Close.material", Player::closeInventory);
        buttonActions.put("messages.gui.Maingui.buttons.Player-Accounts.material", PlayerListGui::openFirstPage);
        buttonActions.put("messages.gui.Maingui.buttons.Player-Login-History.material",
                player -> player.sendMessage(LanguageManager.getMessage("messages.gui.Maingui.buttons.Player-Login-History.name"))); // Logika dla przycsisku 1
        buttonActions.put("messages.gui.Maingui.buttons.Settings.material",
                player -> player.sendMessage(LanguageManager.getMessage("messages.gui.Maingui.buttons.Settings.name")));  // Logika dla przycsisku 2
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(LanguageManager.getMessage("messages.gui.Maingui.name"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            Material clicked = event.getCurrentItem().getType();
            Player player = (Player) event.getWhoClicked();

            buttonActions.forEach((configPath, action) -> {
                if (clicked == Material.valueOf(LanguageManager.getMessage(configPath))) {
                    action.accept(player);
                }
            });
        }
    }
}
