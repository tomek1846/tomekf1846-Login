package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.tomekf1846.Login.Spigot.AdminCommand.Command.AdminCommandChangePassword;
import pl.tomekf1846.Login.Spigot.AdminCommand.Command.AdminCommandEmail;
import pl.tomekf1846.Login.Spigot.MainSpigot;

public class PlayerManageChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerManageState.InputMode mode = PlayerManageState.getInputMode(player);
        if (mode == null) {
            return;
        }

        event.setCancelled(true);
        event.getRecipients().clear();
        String targetName = PlayerManageState.getTarget(player);
        String message = event.getMessage();

        PlayerManageState.clearInput(player);

        Bukkit.getScheduler().runTask(MainSpigot.getInstance(), () -> {
            if (targetName == null || targetName.isBlank()) {
                return;
            }
            if (mode == PlayerManageState.InputMode.EMAIL) {
                AdminCommandEmail.changeEmailFromGui(player, targetName, message);
            } else if (mode == PlayerManageState.InputMode.PASSWORD) {
                AdminCommandChangePassword.changePassword(player, targetName, message);
            }
            PlayerManageGui.openGUI(player, targetName);
        });
    }
}
