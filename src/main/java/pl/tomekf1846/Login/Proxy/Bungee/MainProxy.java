package pl.tomekf1846.Login.Proxy.Bungee;

import net.md_5.bungee.api.plugin.Plugin;
import pl.tomekf1846.Login.Proxy.Bungee.LoginManager.Session.Premium.SessionPremiumManager;
import pl.tomekf1846.Login.Proxy.Bungee.PluginManager.PluginStart;

public class MainProxy extends Plugin {

    @Override
    public void onEnable() {
        PluginStart pluginStart = new PluginStart(this);

        pluginStart.printPluginInfo();
        pluginStart.printLoadingCommand();
        pluginStart.printLoadingSuccess();

        pluginStart.printLoadingListener();
        getProxy().registerChannel("BungeeCord");
        getProxy().getPluginManager().registerListener(this, new SessionPremiumManager());
        pluginStart.printLoadingSuccess();

        pluginStart.printLoadingData();
        PluginStart.StartTime();
        pluginStart.printLoadingSuccess();
    }

    @Override
    public void onDisable() {
        PluginStart pluginStart = new PluginStart(this);
        pluginStart.printPluginInfo();
        pluginStart.printPluginShutdown();
    }
}
