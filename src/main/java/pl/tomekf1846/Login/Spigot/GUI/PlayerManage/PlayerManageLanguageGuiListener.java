package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

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

import java.util.UUID;

public class PlayerManageLanguageGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player viewer = (Player) event.getWhoClicked();
        String targetName = PlayerManageState.getTarget(viewer);
        if (targetName == null) {
            return;
        }
        String title = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.name")
                .replace("{player}", targetName);
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
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String key = container.get(PlayerManageLanguageGui.LANGUAGE_KEY, PersistentDataType.STRING);
        if (key == null) {
            return;
        }
        if ("close".equalsIgnoreCase(key)) {
            viewer.closeInventory();
            return;
        }

        UUID targetUuid = PlayerDataSave.findUUIDByNick(targetName);
        String prefix = LanguageManager.getMessage(viewer, "messages.prefix.main-prefix");
        if (targetUuid == null) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_not_found"));
            viewer.closeInventory();
            return;
        }
        String normalized = LanguageSettings.normalizeLanguage(key);
        if (!LanguageSettings.getLanguageOptions().containsKey(normalized)) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.language_unknown"));
            viewer.closeInventory();
            return;
        }

        PlayerDataSave.setPlayerLanguage(targetUuid, normalized);
        LanguageSettings.LanguageOption option = LanguageSettings.getLanguageOption(normalized);
        String languageName = option != null ? option.commandName() : normalized;
        viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.language_changed")
                .replace("{player}", targetName)
                .replace("{language}", languageName));

        Player targetPlayer = viewer.getServer().getPlayer(targetUuid);
        if (targetPlayer != null) {
            String targetPrefix = LanguageManager.getMessage(targetPlayer, "messages.prefix.main-prefix");
            targetPlayer.sendMessage(targetPrefix + LanguageManager.getMessage(targetPlayer, "messages.player-commands.language_changed")
                    .replace("{language}", languageName));
        }
        PlayerManageGui.openGUI(viewer, targetName);
    }
}
