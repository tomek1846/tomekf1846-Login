package pl.tomekf1846.Login.Spigot.LoginManager.Login;

import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Cracked.SessionCrackedManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLoginManager {

    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");
    private static final Map<Player, Boolean> playerLoginStatus = new HashMap<>();
    private static final Map<UUID, Integer> wrongPasswordAttempts = new HashMap<>();

    public static boolean isPlayerLoggedIn(Player player) {
        return !PlayerRestrictions.isPlayerBlocked(player);
    }

    public static void loginPlayer(Player player, String password) {
        if (isPlayerLoggedIn(player)) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.already_logged_in"));
            return;
        }

        if (!isPlayerRegistered(player)) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.not_registered"));
            return;
        }

        Map<String, String> config = PlayerDataSave.loadPlayerData(player.getUniqueId());
        if (config == null) {
            player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.not_registered"));
            return;
        }
        UUID playerUUID = player.getUniqueId();
        String savedPassword = config.get("Password");
        boolean kickOnWrongPassword = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Wrong-kick-password");
        if (savedPassword == null || !savedPassword.equals(password)) {
            int wrongAttempts = wrongPasswordAttempts.getOrDefault(playerUUID, 0) + 1;
            wrongPasswordAttempts.put(playerUUID, wrongAttempts);
            PlayerDataSave.saveLoginAttempt(player, false, password, wrongAttempts);
            if (kickOnWrongPassword) {
                player.kickPlayer(LanguageManager.getMessage("messages.player-commands.incorrect-password"));
            } else {
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.incorrect-password"));
            }
            return;
        }

        wrongPasswordAttempts.remove(playerUUID);
        PlayerRestrictions.unblockPlayer(player);
        PlayerDataSave.saveLoginAttempt(player, true, null, 0);
        player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.successfully_logged_in"));
        playerLoginStatus.put(player, true);
        SessionCrackedManager.incrementLoginCount(playerUUID);
        LoginMessagesManager.LoginTitle(player);
    }

    public static void forceLoginPlayer(Player player) {
        if (isPlayerLoggedIn(player)) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        wrongPasswordAttempts.remove(playerUUID);
        PlayerRestrictions.unblockPlayer(player);
        PlayerDataSave.saveLoginAttempt(player, true, null, 0);
        player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.successfully_logged_in"));
        playerLoginStatus.put(player, true);
        SessionCrackedManager.incrementLoginCount(playerUUID);
        LoginMessagesManager.LoginTitle(player);
    }

    private static boolean isPlayerRegistered(Player player) {
        return PlayerDataSave.loadPlayerData(player.getUniqueId()) != null;
    }

    public static boolean hasPlayerLoggedIn(Player player) {
        return playerLoginStatus.getOrDefault(player, false);
    }

    public static void removePlayerLoginStatus(Player player) {
        if (hasPlayerLoggedIn(player)) {
            playerLoginStatus.remove(player);
        }
    }

    public static void removeAllPlayerLoginStatus() {
        playerLoginStatus.clear();
        wrongPasswordAttempts.clear();
    }
}
