package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tomekf1846.Login.Spigot.AdminCommand.Command.AdminCommandUnregister;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public class PlayerManageConfirmGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player viewer = (Player) event.getWhoClicked();
        String targetName = PlayerManageState.getTarget(viewer);
        if (targetName == null) {
            return;
        }
        String title = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.name")
                .replace("{player}", targetName);
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.hasDisplayName() ? meta.getDisplayName() : "";
        Material material = clicked.getType();

        if (material == Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Cancel.material"))
                && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Cancel.name"))) {
            viewer.closeInventory();
            PlayerManageGui.openGUI(viewer, targetName);
            return;
        }

        if (material == Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Confirm.material"))
                && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Confirm.name"))) {
            AdminCommandUnregister.unregisterPlayer(viewer, targetName);
            viewer.closeInventory();
        }
    }
}
