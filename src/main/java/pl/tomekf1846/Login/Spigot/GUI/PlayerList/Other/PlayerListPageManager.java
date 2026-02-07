package pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.PlayerListGui;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.Map;
import java.util.UUID;

public class PlayerListPageManager {
    public static final Map<UUID, Integer> playerPages = new java.util.HashMap<>();
    private static final int PLAYERS_PER_PAGE = 36;

    public static ItemStack[] getPlayersForPage(Player viewer, int page) {
        ItemStack[] allPlayerHeads = PlayerListManager.getPlayerHeads(viewer);
        int totalPlayers = allPlayerHeads.length;

        if (totalPlayers == 0) return new ItemStack[0];

        int totalPages = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE);
        if (page > totalPages) return new ItemStack[0];

        int startIndex = (page - 1) * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, totalPlayers);
        if (startIndex >= totalPlayers) return new ItemStack[0];
        ItemStack[] playerHeadsForPage = new ItemStack[endIndex - startIndex];
        System.arraycopy(allPlayerHeads, startIndex, playerHeadsForPage, 0, playerHeadsForPage.length);

        return playerHeadsForPage;
    }


    public static void nextPage(Player player) {
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);
        playerPages.put(player.getUniqueId(), currentPage + 1);
        PlayerListGui.openGUI(player, currentPage + 1);
    }

    public static void previousPage(Player player) {
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);
        if (currentPage > 1) {
            playerPages.put(player.getUniqueId(), currentPage - 1);
            PlayerListGui.openGUI(player, currentPage - 1);
        } else {
            player.sendMessage(LanguageManager.getMessage(player, "messages.prefix.main-prefix")
                    + LanguageManager.getMessage(player, "messages.gui.frist-page"));
        }
    }
}
