package pl.tomekf1846.Login.Spigot.GUI.Language;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageSettings;
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
        if (!LanguageSettings.isPerPlayerLanguageEnabled()) {
            String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.language_command_disabled"));
            player.closeInventory();
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String languageKey = container.get(LanguageGui.LANGUAGE_KEY, PersistentDataType.STRING);
        if ("close".equalsIgnoreCase(languageKey)) {
            player.closeInventory();
            return;
        }
        if (languageKey == null || languageKey.isBlank()) {
            return;
        }
        PlayerDataSave.setPlayerLanguage(player.getUniqueId(), languageKey);
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        String languageName = languageKey;
        var option = LanguageSettings.getLanguageOption(languageKey);
        if (option != null) {
            languageName = option.commandName();
        }
        player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.language_changed")
                .replace("{language}", languageName));
        player.closeInventory();
    }
}
