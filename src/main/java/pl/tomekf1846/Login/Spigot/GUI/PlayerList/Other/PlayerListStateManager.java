package pl.tomekf1846.Login.Spigot.GUI.PlayerList.Other;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import pl.tomekf1846.Login.Spigot.GUI.PlayerList.PlayerListGui;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.List;

public class PlayerListStateManager {

    public static boolean isOnline = true;

    public ItemStack getOnlineItem(Player viewer) {
        String path = isOnline ? "messages.gui.Playerlist.buttons.Onlinedye"
                : "messages.gui.Playerlist.buttons.Offlinedye";
        Material material = Material.matchMaterial(LanguageManager.getMessage(viewer, path + ".material"));
        if (material == null) {
            material = isOnline ? Material.LIME_DYE : Material.GRAY_DYE;
        }
        String name = LanguageManager.getMessage(viewer, path + ".name");
        List<String> lore = LanguageManager.getMessageList(viewer, path + ".lore");
        return createItem(material, name, lore);
    }


    public static void toggleState() {
        isOnline = !isOnline;
    }

    public static void toggleOnlineOffline(Player player) {
        toggleState();
        PlayerListGui.openFirstPage(player);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
