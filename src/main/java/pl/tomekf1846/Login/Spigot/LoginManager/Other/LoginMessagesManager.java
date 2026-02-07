package pl.tomekf1846.Login.Spigot.LoginManager.Other;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BarStyle;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginMessagesManager {
    private static final String PREFIX = LanguageManager.getMessage("messages.prefix.main-prefix");

    private static final Map<UUID, BossBar> bossBars = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> countdownTasks = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> messageTasks = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> titleTasks = new HashMap<>();

    public static void MessagesStart(Player player) {
        Bukkit.getScheduler().runTaskLater(MainSpigot.getInstance(), () -> {
            hideInfo(player);

            FileConfiguration config = MainSpigot.getInstance().getConfig();
            int registerTime = config.getInt("Main-Settings.Time-to-Register");
            int loginTime = config.getInt("Main-Settings.Time-to-Login");

            if (hasPlayerDataFile(player)) {
                showLoginInfo(player, loginTime);
            } else {
                showRegisterInfo(player, registerTime);
            }
        }, 5L);
    }

    public static void hideInfo(Player player) {
        UUID uuid = player.getUniqueId();

        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        BukkitRunnable task = countdownTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        BukkitRunnable msgTask = messageTasks.remove(uuid);
        if (msgTask != null) {
            msgTask.cancel();
        }

        BukkitRunnable titleTask = titleTasks.remove(uuid);
        if (titleTask != null) {
            titleTask.cancel();
        }

        player.sendTitle("", "", 0, 0, 0);
    }

    private static void showRegisterInfo(Player player, int time) {
        UUID uuid = player.getUniqueId();
        if (time > 0) {
            String bossBarMessage = LanguageManager.getMessage("messages.bossbar.registration_time_left").replace("{seconds}", String.valueOf(time));
            BossBar bossBar = Bukkit.createBossBar(bossBarMessage, BarColor.BLUE, BarStyle.SOLID);
            bossBar.setProgress(1.0);
            bossBar.addPlayer(player);
            bossBars.put(uuid, bossBar);
            startCountdown(player, time, true);
        }

        sendRepeatingTitle(player,
                LanguageManager.getMessage("messages.title.useregister.title"),
                LanguageManager.getMessage("messages.title.useregister.subtitle")
        );

        sendRepeatingMessage(player, PREFIX + LanguageManager.getMessage("messages.player-commands.register-message"));
    }

    private static void showLoginInfo(Player player, int time) {
        UUID uuid = player.getUniqueId();
        if (time > 0) {
            String bossBarMessage = LanguageManager.getMessage("messages.bossbar.login_time_left").replace("{seconds}", String.valueOf(time));
            BossBar bossBar = Bukkit.createBossBar(bossBarMessage, BarColor.GREEN, BarStyle.SOLID);
            bossBar.setProgress(1.0);
            bossBar.addPlayer(player);
            bossBars.put(uuid, bossBar);
            startCountdown(player, time, false);
        }

        sendRepeatingTitle(player,
                LanguageManager.getMessage("messages.title.uselogin.title"),
                LanguageManager.getMessage("messages.title.uselogin.subtitle")
        );

        sendRepeatingMessage(player, PREFIX + LanguageManager.getMessage("messages.player-commands.login-message"));
    }

    private static void startCountdown(Player player, int time, boolean isRegister) {
        if (time <= 0) return;
        UUID uuid = player.getUniqueId();

        BukkitRunnable countdownTask = new BukkitRunnable() {
            private int remainingTime = time;

            @Override
            public void run() {
                BossBar bossBar = bossBars.get(uuid);
                if (bossBar == null) {
                    cancel();
                    return;
                }

                if (remainingTime <= 0) {
                    String kickMessageKey = isRegister ? "messages.title.registration-timeout.message" : "messages.title.login-timeout.message";
                    player.kickPlayer(LanguageManager.getMessage(kickMessageKey));
                    hideInfo(player);
                    cancel();
                } else {
                    double progress = (double) remainingTime / time;
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                    String bossBarMessageKey = isRegister ? "messages.title.bossbar.registration_time_left" : "messages.title.bossbar.login_time_left";
                    String bossBarMessage = LanguageManager.getMessage(bossBarMessageKey).replace("{seconds}", String.valueOf(remainingTime));
                    bossBar.setTitle(bossBarMessage);

                    remainingTime--;
                }
            }
        };

        countdownTasks.put(uuid, countdownTask);
        countdownTask.runTaskTimer(MainSpigot.getInstance(), 0L, 20L);
    }

    private static void sendRepeatingTitle(Player player, String title, String subtitle) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable titleTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
        };

        titleTasks.put(uuid, titleTask);
        titleTask.runTaskTimer(MainSpigot.getInstance(), 0L, 40L);
    }

    private static void sendRepeatingMessage(Player player, String message) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable messageTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(message);
            }
        };

        messageTasks.put(uuid, messageTask);
        messageTask.runTaskTimer(MainSpigot.getInstance(), 0L, 100L);
    }

    private static boolean hasPlayerDataFile(Player player) {
        return PlayerDataSave.loadPlayerData(player.getUniqueId()) != null;
    }
    public static void PremiumLogin(Player player) {
        sendTitle(player, "messages.title.premium-login.title", "messages.title.premium-login.subtitle");
    }

    public static void CrackedSessionLoginTitle(Player player) {
        sendTitle(player, "messages.title.cracked-session-login.title", "messages.title.cracked-session-login.subtitle");
    }

    public static void LoginTitle(Player player) {
        sendTitle(player, "messages.title.login.title", "messages.title.login.subtitle");
    }

    public static void LoginAdminTitle(Player player) {
        sendTitle(player, "messages.title.admin-login.title", "messages.title.admin-login.subtitle");
    }

    public static void PremiumRegisterTitle(Player player) {
        sendTitle(player, "messages.title.premium-register.title", "messages.title.premium-register.subtitle");
    }

    public static void CrackedRegisterTitle(Player player) {
        sendTitle(player, "messages.title.cracked-register.title", "messages.title.cracked-register.subtitle");
    }

    private static void sendTitle(Player player, String titlePath, String subtitlePath) {
        String title = LanguageManager.getMessage(titlePath);
        String subtitle = LanguageManager.getMessage(subtitlePath);
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
}
