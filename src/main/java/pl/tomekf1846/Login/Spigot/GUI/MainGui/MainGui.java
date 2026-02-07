package pl.tomekf1846.Login.Spigot.GUI.MainGui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainGui {

    public static void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, LanguageManager.getMessage(player, "messages.gui.Maingui.name"));

        ItemStack A = createGlass(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.filling.filling-A")), "§7");
        ItemStack B = createGlass(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.filling.filling-B")), "§7");
        ItemStack C = createGlass(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.filling.filling-C")), "§7");

        ItemStack[] pattern = {
                A, B, C, B, A, B, C, B, A,
                B, C, B, C, B, C, B, C, B,
                C, B, A, B, C, B, A, B, C
        };
        gui.setContents(pattern);

        gui.setItem(12, createItem(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Player-Accounts.material")),
                LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Player-Accounts.name"),
                getLore(player, "messages.gui.Maingui.buttons.Player-Accounts.lore")));

        gui.setItem(13, createItem(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Player-Login-History.material")),
                LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Player-Login-History.name"),
                getLore(player, "messages.gui.Maingui.buttons.Player-Login-History.lore")));

        gui.setItem(14, createItem(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Settings.material")),
                LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Settings.name"),
                getLore(player, "messages.gui.Maingui.buttons.Settings.lore")));

        gui.setItem(22, createItem(Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Close.material")),
                LanguageManager.getMessage(player, "messages.gui.Maingui.buttons.Close.name"),
                getLore(player, "messages.gui.Maingui.buttons.Close.lore")));

        player.openInventory(gui);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGlass(Material material, String name) {
        return createItem(material, name, List.of());
    }

    private static List<String> getLore(Player player, String basePath) {
        return LanguageManager.getMessageList(player, basePath);
    }
}
