package pl.tomekf1846.Login.Spigot.PlayerCommand.Command;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager.isPasswordTooSimple;

public class PlayerCommandPremiumCracked {
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");
    private static final String PLAYER_DATA_FOLDER = "plugins/tomekf1846-Login/Data/";
    private static final Map<UUID, String> pendingConfirmations = new HashMap<>();
    private static final Map<UUID, Boolean> confirmationType = new HashMap<>();

    public static void handlePremiumCommand(Player player, String password) {
        handleCommand(player, password, true);
    }

    public static void handleCrackedCommand(Player player, String password) {
        FileConfiguration config = MainSpigot.getInstance().getConfig();
        int minPasswordLength = config.getInt("Main-Settings.Password-Requirements.Minimum-length");
        if (password.length() < minPasswordLength) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.password-too-short").replace("{min-length}", String.valueOf(minPasswordLength)));
            return;
        }

        if (isPasswordTooSimple(password)) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.password-too-simple"));
            return;
        }

        handleCommand(player, password, false);
    }

    private static void handleCommand(Player player, String password, boolean premiumStatus) {
        UUID playerUUID = player.getUniqueId();

        if (premiumStatus) {
            Map<String, String> playerData = PlayerDataSave.loadPlayerData(playerUUID);
            if (playerData == null) {
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.data-not-found"));
                return;
            }

            String storedPassword = playerData.get("Password");
            if (storedPassword == null || !storedPassword.equals(password)) {
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.incorrect-password"));
                return;
            }
        }

        if (isAlreadySetToDesiredStatus(playerUUID, premiumStatus)) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.account-already-set").replace("{account_status}", premiumStatus ? "premium" : "cracked"));
            return;
        }

        if (pendingConfirmations.containsKey(playerUUID) && confirmationType.get(playerUUID) == premiumStatus) {
            String storedConfirmation = pendingConfirmations.get(playerUUID);
            if (!premiumStatus || storedConfirmation.equals(password)) {
                pendingConfirmations.remove(playerUUID);
                confirmationType.remove(playerUUID);

                if (!premiumStatus) {
                    PlayerDataSave.setPlayerPassword(playerUUID, password);
                }

                setPlayerStatus(player, premiumStatus);
            } else {
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.passwords-do-not-match"));
                pendingConfirmations.remove(playerUUID);
                confirmationType.remove(playerUUID);
            }
            return;
        }

        pendingConfirmations.put(playerUUID, password);
        confirmationType.put(playerUUID, premiumStatus);
        player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.confirmation-requested").replace("{mode}", premiumStatus ? "premium" : "cracked"));

        Bukkit.getScheduler().runTaskLaterAsynchronously(MainSpigot.getInstance(), () -> {
            pendingConfirmations.remove(playerUUID);
            confirmationType.remove(playerUUID);
        }, 20L * 25);
    }

    private static boolean isAlreadySetToDesiredStatus(UUID playerUUID, boolean premiumStatus) {
        File playerFile = new File(PLAYER_DATA_FOLDER, playerUUID + ".yml");
        if (!playerFile.exists()) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(playerFile.toPath());
            for (String line : lines) {
                if (line.startsWith("Premium: ")) {
                    return line.equals("Premium: " + premiumStatus);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void setPlayerStatus(Player player, boolean premiumStatus) {
        UUID playerUUID = player.getUniqueId();
        File playerFile = new File(PLAYER_DATA_FOLDER, playerUUID + ".yml");

        if (!playerFile.exists()) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.data-not-found"));
            return;
        }

        try {
            List<String> lines = Files.readAllLines(playerFile.toPath());
            boolean updated = false;
            String newStatus = "Premium: " + premiumStatus;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("Premium: ")) {
                    lines.set(i, newStatus);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                lines.add(newStatus);
            }

            Files.write(playerFile.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
            PlayerLoginManager.removePlayerLoginStatus(player);
            SessionCrackedManager.clearLoginCount(player.getUniqueId());

            String kickMessage = premiumStatus
                    ? LanguageManager.getMessage("messages.admin-commands.player_kicked_premium")
                    : LanguageManager.getMessage("messages.admin-commands.player_kicked_cracked");

            player.kickPlayer(kickMessage);
        } catch (IOException e) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.admin-commands.error_updating_player_file"));
            e.printStackTrace();
        }
    }
}
