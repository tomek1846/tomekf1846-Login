package pl.tomekf1846.Login.Spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tomekf1846.Login.Spigot.FileManager.BlockedPasswordManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.GUI.MainGui.MainGuiListener;
import pl.tomekf1846.Login.Spigot.AdminCommand.AdminCommandManager;
import pl.tomekf1846.Login.Spigot.AdminCommand.AdminCommandTabCompleter;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.PlayerListGuiListener;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search.PlayerListSearchListener;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search.PlayerListSearch;
import pl.tomekf1846.Login.Spigot.Listener.PlayerJoinListener;
import pl.tomekf1846.Login.Spigot.Listener.PlayerLeaveListener;
import pl.tomekf1846.Login.Spigot.LoginManager.Login.PlayerLoginManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener.PremiumLoginListener;
import pl.tomekf1846.Login.Spigot.PlayerCommand.Other.PlayerCommandManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener.SuccessPacketListener;
import pl.tomekf1846.Login.Spigot.PlayerCommand.Other.PlayerCommandTabCompleter;
import pl.tomekf1846.Login.Spigot.PluginManager.LicenseManager;
import pl.tomekf1846.Login.Spigot.PluginManager.PluginStart;

import java.util.Objects;

public final class MainSpigot extends JavaPlugin {
    private static MainSpigot instance;
    private static final int BSTATS_PLUGIN_ID = 25273;

    private PremiumLoginListener loginListener;
    private SuccessPacketListener successListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        PlayerDataSave.initialize(this);
        LanguageManager languageManager = new LanguageManager(this);
        PluginStart pluginStart = new PluginStart(this, languageManager);
        AdminCommandManager commandManager = new AdminCommandManager(this, languageManager);
        BlockedPasswordManager.copyBlockedPasswordFile(getDataFolder());
        AdminCommandTabCompleter tabCompleter = new AdminCommandTabCompleter();
        PlayerCommandManager playerCommandManager = new PlayerCommandManager();
        PlayerCommandTabCompleter playerCommandTabCompleter = new PlayerCommandTabCompleter();
        LicenseManager.CheckLicense();
        pluginStart.printPluginInfo();

        pluginStart.printLoadingCommand();
        Objects.requireNonNull(this.getCommand("alogin")).setExecutor(commandManager);
        Objects.requireNonNull(this.getCommand("alogin")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(this.getCommand("login")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("login")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("log")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("log")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("l")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("l")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("register")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("register")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("reg")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("reg")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("cracked")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("cracked")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("premium")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("premium")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("changepass")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("changepass")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("changepassword")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("changepassword")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("email")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("email")).setTabCompleter(playerCommandTabCompleter);
        Objects.requireNonNull(this.getCommand("login-help")).setExecutor(playerCommandManager);
        Objects.requireNonNull(this.getCommand("login-help")).setTabCompleter(playerCommandTabCompleter);
        pluginStart.printLoadingSuccess();

        pluginStart.printLoadingListener();

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        this.loginListener = new PremiumLoginListener(this, pm);
        pm.addPacketListener(this.loginListener);

        this.successListener = new SuccessPacketListener(this, this.loginListener);
        pm.addPacketListener(this.successListener);

        getServer().getPluginManager().registerEvents(new MainGuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListGuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLeaveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRestrictions(), this);
        getServer().getPluginManager().registerEvents(new PlayerListSearch(), this);
        getServer().getPluginManager().registerEvents(new PlayerListSearchListener(), this);
        pluginStart.printLoadingSuccess();

        pluginStart.printLoadingData();
        new Metrics(this, BSTATS_PLUGIN_ID);
        PlayerLoginManager.removeAllPlayerLoginStatus();
        PluginStart.StartTime();
        pluginStart.printLoadingSuccess();
        pluginStart.startkickall();
    }

    @Override
    public void onDisable() {
        LanguageManager languageManager = new LanguageManager(this);
        PluginStart pluginStart = new PluginStart(this, languageManager);

        pluginStart.printPluginInfo();
        pluginStart.printPluginShutdown();
        pluginStart.stopkickall();

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        if (loginListener != null) {
            pm.removePacketListener(loginListener);
            loginListener.clearSessions();
        }
        if (successListener != null) {
            pm.removePacketListener(successListener);
        }
        PlayerDataSave.shutdown();
    }

    public static MainSpigot getInstance() {
        return instance;
    }
}
