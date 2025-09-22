package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium.SessionPremiumCheck;

public class PremiumEligibilityChecker {

    private final Plugin plugin;

    public PremiumEligibilityChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean shouldHandle(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (PlayerDataSave.isPlayerPremium(username)) {
            return true;
        }

        FileConfiguration config = plugin.getConfig();
        boolean premiumCommandEnabled = config.getBoolean("Main-Settings.Premium-Command", false);
        if (!premiumCommandEnabled) {
            return SessionPremiumCheck.isPlayerPremium(username);
        }

        return false;
    }
}