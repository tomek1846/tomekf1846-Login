package pl.tomekf1846.Login.Spigot.PlayerCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.util.*;

import static pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager.isPasswordTooSimple;

public class PlayerCommandPremiumCracked {
    private static final Map<UUID, String> pendingConfirmations = new HashMap<>();
    private static final Map<UUID, Boolean> confirmationType = new HashMap<>();

    public static void handlePremiumCommand(Player player, String password) {
        handleCommand(player, password, true);
    }

    public static void handleCrackedCommand(Player player, String password) {
        FileConfiguration config = MainSpigot.getInstance().getConfig();
        int minPasswordLength = config.getInt("Main-Settings.Password-Requirements.Minimum-length");
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        if (password.length() < minPasswordLength) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.password-too-short")
                    .replace("{min-length}", String.valueOf(minPasswordLength)));
            return;
        }

        if (isPasswordTooSimple(password)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.password-too-simple"));
            return;
        }

        handleCommand(player, password, false);
    }

    private static void handleCommand(Player player, String password, boolean premiumStatus) {
        UUID playerUUID = player.getUniqueId();
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");

        if (premiumStatus) {
            Map<String, String> playerData = PlayerDataSave.loadPlayerData(playerUUID);
            if (playerData == null) {
                player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.data-not-found"));
                return;
            }

            String storedPassword = playerData.get("Password");
            if (storedPassword == null || !PasswordSecurity.matches(password, storedPassword)) {
                player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.incorrect-password"));
                return;
            }
        }

        if (isAlreadySetToDesiredStatus(playerUUID, premiumStatus)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.account-already-set")
                    .replace("{account_status}", premiumStatus ? "premium" : "cracked"));
            return;
        }

        if (pendingConfirmations.containsKey(playerUUID) && confirmationType.get(playerUUID) == premiumStatus) {
            String storedConfirmation = pendingConfirmations.get(playerUUID);
            if (!premiumStatus || storedConfirmation.equals(password)) {
                pendingConfirmations.remove(playerUUID);
                confirmationType.remove(playerUUID);

                if (!premiumStatus) {
                    PasswordSecurity.encodeAsync(MainSpigot.getInstance(), password, encodedPassword -> {
                        PlayerDataSave.setPlayerPassword(playerUUID, encodedPassword);
                    });
                }

                setPlayerStatus(player, premiumStatus);
            } else {
                player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.passwords-do-not-match"));
                pendingConfirmations.remove(playerUUID);
                confirmationType.remove(playerUUID);
            }
            return;
        }

        pendingConfirmations.put(playerUUID, password);
        confirmationType.put(playerUUID, premiumStatus);
        player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.confirmation-requested")
                .replace("{mode}", premiumStatus ? "premium" : "cracked"));

        Bukkit.getScheduler().runTaskLaterAsynchronously(MainSpigot.getInstance(), () -> {
            pendingConfirmations.remove(playerUUID);
            confirmationType.remove(playerUUID);
        }, 20L * 25);
    }

    private static boolean isAlreadySetToDesiredStatus(UUID playerUUID, boolean premiumStatus) {
        Map<String, String> playerData = PlayerDataSave.loadPlayerData(playerUUID);
        if (playerData == null) {
            return false;
        }
        String premiumValue = playerData.get("Premium");
        return premiumValue != null && premiumValue.equalsIgnoreCase(String.valueOf(premiumStatus));
    }

    private static void setPlayerStatus(Player player, boolean premiumStatus) {
        UUID playerUUID = player.getUniqueId();
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        if (!PlayerDataSave.setPlayerSession(player.getName(), premiumStatus)) {
            player.sendMessage(prefix + LanguageManager.getMessage(player, "messages.player-commands.data-not-found"));
            return;
        }

        PlayerLoginManager.removePlayerLoginStatus(player);
        SessionCrackedManager.clearLoginCount(player.getUniqueId());

        String kickMessage = premiumStatus
                ? LanguageManager.getMessage(player, "messages.admin-commands.player_kicked_premium")
                : LanguageManager.getMessage(player, "messages.admin-commands.player_kicked_cracked");

        player.kickPlayer(kickMessage);
    }
}
