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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

public class AdminCommandPremiumCracked {
    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());
    private static final String PREFIX = languageManager.getMessage("messages.prefix.main-prefix");
    private static final String PLAYER_DATA_FOLDER = "plugins/tomekf1846-Login/Data/";

    public static void setPlayerPremium(CommandSender sender, String playerName) {
        setPlayerStatus(sender, playerName, true, null);
    }

    public static void setPlayerCracked(CommandSender sender, String playerName, String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.usage_cracked"));
            return;
        }
        setPlayerStatus(sender, playerName, false, newPassword);
        Player player = Bukkit.getServer().getPlayer(playerName);
        if (player != null) player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    private static void setPlayerStatus(CommandSender sender, String playerName, boolean premiumStatus, String newPassword) {
        if (sender.getName().equalsIgnoreCase(playerName)) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.cannot_change_own_status"));
            return;
        }

        boolean isPremiumCommandEnabled = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Premium-Command");
        if (!isPremiumCommandEnabled) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.premium_command_disabled"));
            return;
        }

        UUID playerUUID = NickUuidCheck.getUUIDFromNick(playerName);

        if (playerUUID == null) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.player_not_found"));
            return;
        }

        File playerFile = new File(PLAYER_DATA_FOLDER, playerUUID + ".yml");
        if (!playerFile.exists()) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.player_file_not_found"));
            return;
        }

        try {
            List<String> lines = Files.readAllLines(playerFile.toPath());
            boolean alreadySet = false;
            boolean updated = false;
            String newStatus = "Premium: " + premiumStatus;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("Premium: ")) {
                    if (lines.get(i).equals(newStatus)) {
                        alreadySet = true;
                        break;
                    }
                    lines.set(i, newStatus);
                    updated = true;
                    break;
                }
            }

            if (alreadySet) {
                sender.sendMessage(PREFIX + languageManager.getMessage(
                        premiumStatus ? "messages.admin-commands.already_premium" : "messages.admin-commands.already_cracked"
                ));
                return;
            }

            if (!premiumStatus && newPassword != null) {
                PlayerDataSave.setPlayerPassword(playerUUID, newPassword);
            }

            if (updated) {
                Files.write(playerFile.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
                sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.player_status_updated")
                        .replace("{status}", premiumStatus ? "Premium" : "Cracked")
                        .replace("{player}", playerName));

                Player player = Bukkit.getPlayer(playerName);
                if (player != null) player.removePotionEffect(PotionEffectType.BLINDNESS);
                if (player != null && player.isOnline()) {
                    player.kickPlayer(languageManager.getMessage(
                            premiumStatus ? "messages.admin-commands.player_kicked_premium" : "messages.admin-commands.player_kicked_cracked"
                    ));
                    PlayerLoginManager.removePlayerLoginStatus(player);
                }
            } else {
                sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.failed_to_update_player"));
            }
        } catch (IOException e) {
            sender.sendMessage(PREFIX + languageManager.getMessage("messages.admin-commands.error_updating_player_file"));
            e.printStackTrace();
        }
    }
}
