package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import pl.tomekf1846.Login.Spigot.AdminCommand.Command.AdminCommandForceLogin;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.Map;
import java.util.UUID;

public class PlayerManageGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player viewer = (Player) event.getWhoClicked();
        String targetName = PlayerManageState.getTarget(viewer);
        if (targetName == null) {
            return;
        }

        String expectedTitle = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.name")
                .replace("{player}", targetName);
        if (!event.getView().getTitle().equals(expectedTitle)) {
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

        if (material == Material.BARRIER && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Close.name"))) {
            viewer.closeInventory();
            return;
        }

        if (material == Material.REPEATER && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Email.name"))) {
            PlayerManageState.startInput(viewer, targetName, PlayerManageState.InputMode.EMAIL);
            viewer.closeInventory();
            viewer.sendMessage(LanguageManager.getMessage(viewer, "messages.prefix.main-prefix")
                    + LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.input.email.message")
                    .replace("{player}", targetName));
            return;
        }

        if (material == Material.COMPARATOR && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Password.name"))) {
            PlayerManageState.startInput(viewer, targetName, PlayerManageState.InputMode.PASSWORD);
            viewer.closeInventory();
            viewer.sendMessage(LanguageManager.getMessage(viewer, "messages.prefix.main-prefix")
                    + LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.input.password.message")
                    .replace("{player}", targetName));
            return;
        }

        if (material == Material.LIME_DYE && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Forcelogin.name"))) {
            AdminCommandForceLogin.forceLogin(viewer, targetName);
            return;
        }

        if (material == Material.YELLOW_DYE && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Language.name"))) {
            PlayerManageLanguageGui.openGUI(viewer, targetName);
            return;
        }

        if (material == Material.PAPER && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Unregister.name"))) {
            PlayerManageConfirmGui.openGUI(viewer, targetName);
            return;
        }

        if (material == Material.RED_DYE && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.IP-List.name"))) {
            return;
        }

        if ((material == Material.DEAD_BUSH || material == Material.TORCHFLOWER)
                && name.equals(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Premium.name"))) {
            togglePremium(viewer, targetName);
        }
    }

    private void togglePremium(Player viewer, String targetName) {
        String prefix = LanguageManager.getMessage(viewer, "messages.prefix.main-prefix");
        if (viewer.getName().equalsIgnoreCase(targetName)) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.cannot_change_own_status"));
            return;
        }

        boolean isPremiumCommandEnabled = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Premium-Command");
        if (!isPremiumCommandEnabled) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.premium_command_disabled"));
            viewer.closeInventory();
            return;
        }

        UUID targetUuid = PlayerDataSave.findUUIDByNick(targetName);
        if (targetUuid == null) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_not_found"));
            return;
        }

        Map<String, String> data = PlayerDataSave.loadPlayerData(targetUuid);
        if (data == null) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_file_not_found"));
            return;
        }

        boolean currentPremium = "true".equalsIgnoreCase(data.get("Premium"));
        boolean newStatus = !currentPremium;

        if (PlayerDataSave.setPlayerSession(targetName, newStatus)) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_status_updated")
                    .replace("{status}", newStatus ? "Premium" : "Cracked")
                    .replace("{player}", targetName));

            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null) {
                targetPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
                if (targetPlayer.isOnline()) {
                    targetPlayer.kickPlayer(LanguageManager.getMessage(targetPlayer,
                            newStatus ? "messages.admin-commands.player_kicked_premium" : "messages.admin-commands.player_kicked_cracked"));
                    PlayerLoginManager.removePlayerLoginStatus(targetPlayer);
                }
            }

            PlayerManageGui.openGUI(viewer, targetName);
        } else {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.failed_to_update_player"));
        }
    }
}
