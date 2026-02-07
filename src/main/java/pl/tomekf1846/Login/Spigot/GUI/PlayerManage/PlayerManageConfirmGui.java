package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

import java.util.List;

public class PlayerManageConfirmGui {

    public static void openGUI(Player viewer, String targetName) {
        String title = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.name")
                .replace("{player}", targetName);
        String layout = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.layout");
        int size = layout.length();
        if (size == 0 || size % 9 != 0) {
            size = 27;
            layout = "RRRRRRRRRRRCAARYRRRRRRRRRRR";
        }
        Inventory gui = Bukkit.createInventory(null, size, title);

        ItemStack glass = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.filling.filling-A")),
                "§7",
                List.of()
        );
        ItemStack redFill = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.filling.filling-R")),
                "§7",
                List.of()
        );
        ItemStack greenFill = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.filling.filling-G")),
                "§7",
                List.of()
        );
        ItemStack cancel = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Cancel.material")),
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Cancel.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Cancel.lore")
        );
        ItemStack confirm = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Confirm.material")),
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Confirm.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.ConfirmUnregister.buttons.Confirm.lore")
        );

        for (int i = 0; i < layout.length() && i < gui.getSize(); i++) {
            char symbol = layout.charAt(i);
            switch (symbol) {
                case 'A' -> gui.setItem(i, glass);
                case 'R' -> gui.setItem(i, redFill);
                case 'G' -> gui.setItem(i, greenFill);
                case 'C' -> gui.setItem(i, cancel);
                case 'Y' -> gui.setItem(i, confirm);
                default -> {
                }
            }
        }

        PlayerManageState.setTarget(viewer, targetName);
        viewer.openInventory(gui);
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
}
