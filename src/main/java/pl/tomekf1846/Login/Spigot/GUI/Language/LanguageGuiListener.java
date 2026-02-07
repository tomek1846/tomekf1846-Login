package pl.tomekf1846.Login.Spigot.GUI.Language;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

public class LanguageGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = LanguageManager.getMessage(player, "messages.gui.Language.name");
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        String languageKey = meta.getPersistentDataContainer().get(LanguageGui.LANGUAGE_KEY, PersistentDataType.STRING);
        if (languageKey != null) {
            PlayerDataSave.setPlayerLanguage(player.getUniqueId(), languageKey);
            player.sendMessage(LanguageManager.getMessage(player, "messages.prefix.main-prefix")
                    + LanguageManager.getMessage(player, "messages.player-commands.language_changed")
                    .replace("{language}", LanguageManager.getMessage(player, "messages.languages." + languageKey + ".name")));
            player.closeInventory();
            return;
        }

        Material closeMaterial = Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Language.buttons.Close.material"));
        String closeName = LanguageManager.getMessage(player, "messages.gui.Language.buttons.Close.name");
        if (clicked.getType() == closeMaterial && closeName.equals(meta.getDisplayName())) {
            player.closeInventory();
        }
    }
}
