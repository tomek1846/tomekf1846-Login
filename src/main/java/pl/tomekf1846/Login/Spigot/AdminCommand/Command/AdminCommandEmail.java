package pl.tomekf1846.Login.Spigot.AdminCommand.Command;

import org.bukkit.command.CommandSender;
import pl.tomekf1846.Login.Spigot.FileManager.NickUuidCheck;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.UUID;
import java.util.HashMap;

public class AdminCommandEmail {
    private static final LanguageManager languageManager = new LanguageManager(MainSpigot.getInstance());
    private static final HashMap<String, Long> pendingConfirmations = new HashMap<>();
    private static final HashMap<String, String> pendingEmails = new HashMap<>();

    public static void changeEmail(CommandSender sender, String playerName, String newEmail) {
        UUID uuid = NickUuidCheck.getUUIDFromNick(playerName);
        String prefix = languageManager.getMessage("messages.prefix.main-prefix");

        if (uuid == null) {
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.player_not_found"));
            return;
        }

        if ("(none)".equalsIgnoreCase(newEmail) || "none".equalsIgnoreCase(newEmail)) {
            PlayerDataSave.setPlayerEmail(uuid, "none");
            sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.email_removed").replace("{player}", playerName));
            return;
        }

        int atIndex = newEmail.indexOf("@");
        int lastDotIndex = newEmail.lastIndexOf(".");
        if (newEmail.length() < 3 ||
                !newEmail.contains("@") ||
                newEmail.chars().filter(ch -> ch == '@').count() > 1 ||
                newEmail.startsWith("@") ||
                newEmail.contains("@.") ||
                lastDotIndex == -1 ||
                atIndex > lastDotIndex ||
                lastDotIndex - atIndex < 3) {
            String key = sender.getName() + ":" + playerName;
            MainSpigot instance = MainSpigot.getInstance();

            if (!pendingEmails.containsKey(key) || !pendingEmails.get(key).equals(newEmail)) {
                pendingConfirmations.put(key, System.currentTimeMillis());
                pendingEmails.put(key, newEmail);
                instance.getServer().getScheduler().runTaskLater(instance, () -> {
                    pendingConfirmations.remove(key);
                    pendingEmails.remove(key);
                }, 300);
                sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.invalid_email"));
                return;
            }

            if (System.currentTimeMillis() - pendingConfirmations.get(key) <= 15000) {
                PlayerDataSave.setPlayerEmail(uuid, newEmail);
                sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.email_changed")
                        .replace("{player}", playerName)
                        .replace("{email}", newEmail));
                pendingConfirmations.remove(key);
                pendingEmails.remove(key);
                return;
            }
        }

        PlayerDataSave.setPlayerEmail(uuid, newEmail);
        sender.sendMessage(prefix + languageManager.getMessage("messages.admin-commands.email_changed")
                .replace("{player}", playerName)
                .replace("{email}", newEmail));
    }
}
