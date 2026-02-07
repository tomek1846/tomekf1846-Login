package pl.tomekf1846.Login.Spigot.GUI.Language;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageSettings;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LanguageGui {
    public static final NamespacedKey LANGUAGE_KEY = new NamespacedKey(MainSpigot.getInstance(), "language_key");

    public static void openGUI(Player player) {
        String title = LanguageManager.getMessage(player, "messages.gui.Language.name");
        String layout = LanguageManager.getMessage(player, "messages.gui.Language.layout");
        int size = layout.length();
        if (size == 0 || size % 9 != 0) {
            size = 27;
            layout = "ABCABCABC000000000000000000";
        }
        Inventory gui = Bukkit.createInventory(null, size, title);

        ItemStack a = createGlass(player, "messages.gui.Language.filling.filling-A");
        ItemStack b = createGlass(player, "messages.gui.Language.filling.filling-B");
        ItemStack c = createGlass(player, "messages.gui.Language.filling.filling-C");
        ItemStack close = createItem(
                Material.valueOf(LanguageManager.getMessage(player, "messages.gui.Language.buttons.Close.material")),
                LanguageManager.getMessage(player, "messages.gui.Language.buttons.Close.name"),
                LanguageManager.getMessageList(player, "messages.gui.Language.buttons.Close.lore")
        );
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.getPersistentDataContainer().set(LANGUAGE_KEY, PersistentDataType.STRING, "close");
            close.setItemMeta(closeMeta);
        }

        List<ItemStack> languageItems = buildLanguageItems(player);
        int languageIndex = 0;

        for (int i = 0; i < layout.length() && i < gui.getSize(); i++) {
            char symbol = layout.charAt(i);
            switch (symbol) {
                case 'A' -> gui.setItem(i, a);
                case 'B' -> gui.setItem(i, b);
                case 'C' -> gui.setItem(i, c);
                case 'X' -> gui.setItem(i, close);
                case 'L' -> {
                    if (languageIndex < languageItems.size()) {
                        gui.setItem(i, languageItems.get(languageIndex));
                        languageIndex++;
                    }
                }
                default -> {
                }
            }
        }

        player.openInventory(gui);
    }

    private static ItemStack createGlass(Player player, String path) {
        return createItem(Material.valueOf(LanguageManager.getMessage(player, path)), "§7", List.of());
    }

    private static List<ItemStack> buildLanguageItems(Player player) {
        Map<String, LanguageSettings.LanguageOption> options = LanguageSettings.getLanguageOptions();
        List<ItemStack> items = new ArrayList<>();
        String storedLanguage = PlayerDataSave.getPlayerLanguage(player.getUniqueId());
        String playerLanguage = storedLanguage;
        if (playerLanguage == null || playerLanguage.isBlank()) {
            playerLanguage = MainSpigot.getInstance().getConfig().getString("Main-Settings.Language", "English");
        }
        playerLanguage = LanguageSettings.normalizeLanguage(playerLanguage);

        for (Map.Entry<String, LanguageSettings.LanguageOption> entry : options.entrySet()) {
            String key = entry.getKey();
            LanguageSettings.LanguageOption option = entry.getValue();
            String name = LanguageManager.getMessage(player, "messages.gui.Language.languages." + key + ".name");
            List<String> lore = LanguageManager.getMessageList(player, "messages.gui.Language.languages." + key + ".lore");
            String statusKey = playerLanguage.equals(key) ? "messages.gui.Language.status.current" : "messages.gui.Language.status.available";
            String status = LanguageManager.getMessage(player, statusKey);
            List<String> resolvedLore = lore.stream()
                    .map(line -> line.replace("{status}", status).replace("{language}", option.commandName()))
                    .toList();
            ItemStack head = createSkull(name, resolvedLore, option.texture(), option.owner());
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(LANGUAGE_KEY, PersistentDataType.STRING, key);
                head.setItemMeta(meta);
            }
            items.add(head);
        }
        return items;
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

    private static ItemStack createSkull(String name, List<String> lore, String texture, String owner) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (owner != null && !owner.isBlank()) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            } else if (texture != null && !texture.isBlank()) {
                applyTexture(meta, texture);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private static void applyTexture(SkullMeta meta, String texture) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", texture));
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
}
