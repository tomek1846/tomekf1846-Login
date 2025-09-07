package pl.tomekf1846.Login.Spigot.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

import static pl.tomekf1846.Login.Spigot.GUI.PlayerList.Search.PlayerListSearch.searchingPlayer;

public class PlayerLeaveListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (searchingPlayer != null && searchingPlayer.equals(event.getPlayer())) {
            searchingPlayer = null;
        }
        PlayerDataSave.savePlayerLeaveTime(player);
    }
}