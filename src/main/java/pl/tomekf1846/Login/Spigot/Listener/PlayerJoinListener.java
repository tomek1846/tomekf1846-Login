package pl.tomekf1846.Login.Spigot.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.MainLoginManager;

public class PlayerJoinListener implements Listener {

    public PlayerJoinListener() {}

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        MainLoginManager.LoginRegisterMainManger(event);
    }
}
