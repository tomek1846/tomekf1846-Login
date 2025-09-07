package pl.tomekf1846.Login.Spigot.LoginManager.Other;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium.SessionPremiumCheck;
import pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.security.SecureRandom;
import java.util.UUID;

public class MainLoginManager {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");

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
                    PlayerRestrictions.blockPlayer(player);
                    PlayerDataSave.setPlayerPassword(playerUUID, newPassword);
                    PlayerDataSave.setPlayerSession(playerName, false);

                    player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.premium-login-disabled").replace("{new_password}", newPassword));
                }
            } else {
                PlayerRestrictions.blockPlayer(player);
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.login-message"));
            }
        } else {
            if (SessionPremiumCheck.isPlayerPremium(playerName)) {
                if (isPremiumCommandEnabled) {
                    SessionPremiumCheck.handlePremiumRegister(player);
                } else {
                    PlayerRestrictions.blockPlayer(player);
                    player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.register-message"));
                }
            } else {
                PlayerRestrictions.blockPlayer(player);
                player.sendMessage(PREFIX + LanguageManager.getMessage("messages.player-commands.register-message"));
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
}
