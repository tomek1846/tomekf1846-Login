package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageSettings;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManageGui {

    public static void openGUI(Player viewer, String targetName) {
        UUID targetUuid = PlayerDataSave.findUUIDByNick(targetName);
        String prefix = LanguageManager.getMessage(viewer, "messages.prefix.main-prefix");
        if (targetUuid == null) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_not_found"));
            return;
        }
        Map<String, String> data = PlayerDataSave.loadPlayerData(targetUuid);
        if (data == null) {
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.player_file_not_found"));
            return;
        }

        String title = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.name")
                .replace("{player}", targetName);
        String layout = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.layout");
        int size = layout.length();
        if (size == 0 || size % 9 != 0) {
            size = 54;
            layout = "ABCB1BCBABCBACABCBCB2B3B4BCABC5A6CBAB7BACAB8BCBAB9BABC";
        }
        Inventory gui = Bukkit.createInventory(null, size, title);

        ItemStack a = createGlass(viewer, "messages.gui.PlayerManage.filling.filling-A");
        ItemStack b = createGlass(viewer, "messages.gui.PlayerManage.filling.filling-B");
        ItemStack c = createGlass(viewer, "messages.gui.PlayerManage.filling.filling-C");

        ItemStack playerHead = buildPlayerHead(viewer, targetName, targetUuid, data);
        ItemStack emailItem = buildEmailItem(viewer, data.get("Email"));
        ItemStack forceloginItem = buildForceloginItem(viewer);
        ItemStack passwordItem = buildPasswordItem(viewer, data.get("Password"));
        ItemStack languageItem = buildLanguageItem(viewer, targetUuid);
        ItemStack ipItem = buildIpItem(viewer);
        ItemStack premiumItem = buildPremiumItem(viewer, data.get("Premium"));
        ItemStack unregisterItem = buildUnregisterItem(viewer);
        ItemStack closeItem = buildCloseItem(viewer);

        for (int i = 0; i < layout.length() && i < gui.getSize(); i++) {
            char symbol = layout.charAt(i);
            switch (symbol) {
                case 'A' -> gui.setItem(i, a);
                case 'B' -> gui.setItem(i, b);
                case 'C' -> gui.setItem(i, c);
                case '1' -> gui.setItem(i, playerHead);
                case '2' -> gui.setItem(i, emailItem);
                case '3' -> gui.setItem(i, forceloginItem);
                case '4' -> gui.setItem(i, passwordItem);
                case '5' -> gui.setItem(i, languageItem);
                case '6' -> gui.setItem(i, ipItem);
                case '7' -> gui.setItem(i, premiumItem);
                case '8' -> gui.setItem(i, unregisterItem);
                case '9' -> gui.setItem(i, closeItem);
                default -> {
                }
            }
        }

        PlayerManageState.setTarget(viewer, targetName);
        viewer.openInventory(gui);
    }

    private static ItemStack buildPlayerHead(Player viewer, String targetName, UUID targetUuid, Map<String, String> data) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.name")
                    .replace("{player}", targetName));
            List<String> lore = new ArrayList<>();
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.uuid")
                    .replace("{uuid}", getOrDefault(viewer, data.get("Player-UUID"))));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.password")
                    .replace("{password}", getPasswordForDisplay(viewer, data.get("Password"))));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.fristip")
                    .replace("{fristip}", getOrDefault(viewer, data.get("FirstIP"))));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.lastip")
                    .replace("{lastip}", getOrDefault(viewer, data.get("LastIP"))));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.email")
                    .replace("{email}", getOrDefault(viewer, data.get("Email"))));
            lore.add(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.player.lore.premium")
                    .replace("{premium}", getOrDefault(viewer, data.get("Premium"))));
            meta.setLore(lore);
            try {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
                meta.setOwningPlayer(offlinePlayer);
            } catch (Exception ignored) {
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack buildEmailItem(Player viewer, String email) {
        ItemStack item = new ItemStack(Material.REPEATER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Email.name"));
            List<String> lore = LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Email.lore").stream()
                    .map(line -> line.replace("{email}", getOrDefault(viewer, email)))
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildForceloginItem(Player viewer) {
        return createItem(
                Material.LIME_DYE,
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Forcelogin.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Forcelogin.lore")
        );
    }

    private static ItemStack buildPasswordItem(Player viewer, String password) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Password.name"));
            String displayed = getPasswordForDisplay(viewer, password);
            List<String> lore = LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Password.lore").stream()
                    .map(line -> line.replace("{password}", displayed))
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildLanguageItem(Player viewer, UUID targetUuid) {
        ItemStack item = new ItemStack(Material.YELLOW_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Language.name"));
            String stored = PlayerDataSave.getPlayerLanguage(targetUuid);
            if (stored == null || stored.isBlank()) {
                stored = MainSpigot.getInstance().getConfig().getString("Main-Settings.Language", "English");
            }
            String normalized = LanguageSettings.normalizeLanguage(stored);
            LanguageSettings.LanguageOption option = LanguageSettings.getLanguageOption(normalized);
            String languageName = option != null ? option.commandName() : normalized;
            List<String> lore = LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Language.lore").stream()
                    .map(line -> line.replace("{language}", languageName))
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildIpItem(Player viewer) {
        return createItem(
                Material.RED_DYE,
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.IP-List.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.IP-List.lore")
        );
    }

    private static ItemStack buildPremiumItem(Player viewer, String premium) {
        boolean isPremium = premium != null && premium.equalsIgnoreCase("true");
        Material material = isPremium ? Material.TORCHFLOWER : Material.CACTUS_FLOWER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Premium.name"));
            String statusKey = isPremium
                    ? "messages.gui.PlayerManage.buttons.Premium.status.premium"
                    : "messages.gui.PlayerManage.buttons.Premium.status.cracked";
            String status = LanguageManager.getMessage(viewer, statusKey);
            List<String> lore = LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Premium.lore").stream()
                    .map(line -> line.replace("{status}", status))
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildUnregisterItem(Player viewer) {
        return createItem(
                Material.PAPER,
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Unregister.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Unregister.lore")
        );
    }

    private static ItemStack buildCloseItem(Player viewer) {
        return createItem(
                Material.BARRIER,
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.buttons.Close.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.buttons.Close.lore")
        );
    }

    private static ItemStack createGlass(Player viewer, String path) {
        return createItem(Material.valueOf(LanguageManager.getMessage(viewer, path)), "§7", List.of());
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

    private static String getOrDefault(Player viewer, String value) {
        return (value == null || value.trim().isEmpty())
                ? LanguageManager.getMessage(viewer, "messages.gui.no-data")
                : value;
    }

    private static String getPasswordForDisplay(Player viewer, String value) {
        if (value == null || value.trim().isEmpty()) {
            return LanguageManager.getMessage(viewer, "messages.gui.no-data");
        }
        String formatted = PasswordSecurity.formatForDisplay(value);
        return formatted == null || formatted.isBlank()
                ? LanguageManager.getMessage(viewer, "messages.gui.no-data")
                : formatted;
    }
}
