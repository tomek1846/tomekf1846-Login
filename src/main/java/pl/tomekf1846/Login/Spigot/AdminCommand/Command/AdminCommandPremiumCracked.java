package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import java.util.UUID;

public class AdminCommandPremiumCracked {
    public static void setPlayerPremium(CommandSender sender, String playerName) {
        setPlayerStatus(sender, playerName, true, null);
    }

    public static void setPlayerCracked(CommandSender sender, String playerName, String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.usage_cracked"));
            return;
        }
        setPlayerStatus(sender, playerName, false, newPassword);
        Player player = Bukkit.getServer().getPlayer(playerName);
        if (player != null) player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    private static void setPlayerStatus(CommandSender sender, String playerName, boolean premiumStatus, String newPassword) {
        if (sender.getName().equalsIgnoreCase(playerName)) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.cannot_change_own_status"));
            return;
        }

        boolean isPremiumCommandEnabled = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Premium-Command");
        if (!isPremiumCommandEnabled) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.premium_command_disabled"));
            return;
        }

        UUID playerUUID = NickUuidCheck.getUUIDFromNick(playerName);

        if (playerUUID == null) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.player_not_found"));
            return;
        }

        var playerData = PlayerDataSave.loadPlayerData(playerUUID);
        if (playerData == null) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.player_file_not_found"));
            return;
        }

        String currentPremium = playerData.get("Premium");
        if (currentPremium != null && currentPremium.equalsIgnoreCase(String.valueOf(premiumStatus))) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender,
                    premiumStatus ? "messages.admin-commands.already_premium" : "messages.admin-commands.already_cracked"
            ));
            return;
        }

        if (!premiumStatus && newPassword != null) {
            PlayerDataSave.setPlayerPassword(playerUUID, newPassword);
        }

        if (PlayerDataSave.setPlayerSession(playerName, premiumStatus)) {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.player_status_updated")
                    .replace("{status}", premiumStatus ? "Premium" : "Cracked")
                    .replace("{player}", playerName));

            Player player = Bukkit.getPlayer(playerName);
            if (player != null) player.removePotionEffect(PotionEffectType.BLINDNESS);
            if (player != null && player.isOnline()) {
                player.kickPlayer(LanguageManager.getMessage(player,
                        premiumStatus ? "messages.admin-commands.player_kicked_premium" : "messages.admin-commands.player_kicked_cracked"
                ));
                PlayerLoginManager.removePlayerLoginStatus(player);
            }
        } else {
            sender.sendMessage(getPrefix(sender) + LanguageManager.getMessage(sender, "messages.admin-commands.failed_to_update_player"));
        }
    }

    private static String getPrefix(CommandSender sender) {
        return LanguageManager.getMessage(sender, "messages.prefix.main-prefix");
    }
}
