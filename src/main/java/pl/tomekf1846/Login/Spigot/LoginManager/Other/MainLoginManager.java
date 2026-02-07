package pl.tomekf1846.Login.Spigot.LoginManager.Other;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium.SessionPremiumCheck;
import pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.security.SecureRandom;
import java.util.UUID;

public class MainLoginManager {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static void LoginRegisterMainManger(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();
        boolean isPremiumCommandEnabled = MainSpigot.getInstance().getConfig().getBoolean("Main-Settings.Premium-Command");

        if (PlayerRegisterManager.isPlayerRegistered(player)) {
            if (PlayerDataSave.isPlayerPremium(playerName)) {
                if (isPremiumCommandEnabled) {
                    LoginMessagesManager.PremiumLogin(player);
                } else {
                    String newPassword = generateRandomPassword();
                    PlayerDataSave.setPlayerPassword(playerUUID, newPassword);
                    PlayerDataSave.setPlayerSession(playerName, false);
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.premium-login-disabled")
                            .replace("{new_password}", newPassword));
                    PlayerLoginManager.forceLoginPlayer(player);
                }
            } else {
                PlayerRestrictions.blockPlayer(player);
                player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.login-message"));
            }
        } else {
            if (SessionPremiumCheck.isPlayerPremium(playerName)) {
                if (isPremiumCommandEnabled) {
                    SessionPremiumCheck.handlePremiumRegister(player);
                } else {
                    PlayerRestrictions.blockPlayer(player);
                    player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.register-message"));
                }
            } else {
                PlayerRestrictions.blockPlayer(player);
                player.sendMessage(getPrefix(player) + LanguageManager.getMessage(player, "messages.player-commands.register-message"));
            }
        }
    }

    private static String generateRandomPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            password.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    private static String getPrefix(Player player) {
        return LanguageManager.getMessage(player, "messages.prefix.main-prefix");
    }
}
