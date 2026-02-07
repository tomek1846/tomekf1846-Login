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
import pl.tomekf1846.Login.Spigot.MainSpigot;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class LanguageGui {
    public static final NamespacedKey LANGUAGE_KEY = new NamespacedKey(MainSpigot.getInstance(), "language-key");

    public static void openGUI(Player player) {
        int size = parseInt(LanguageManager.getMessage(player, "messages.gui.Language.size"), 27);
        Inventory gui = Bukkit.createInventory(null, size, LanguageManager.getMessage(player, "messages.gui.Language.name"));

        ItemStack fillerA = createGlass(player, "messages.gui.Language.filling.filling-A");
        ItemStack fillerB = createGlass(player, "messages.gui.Language.filling.filling-B");
        ItemStack fillerC = createGlass(player, "messages.gui.Language.filling.filling-C");

        ItemStack[] pattern = {
                fillerA, fillerB, fillerC, fillerB, fillerA, fillerB, fillerC, fillerB, fillerA,
                fillerB, fillerC, fillerB, fillerC, fillerB, fillerC, fillerB, fillerC, fillerB,
                fillerC, fillerB, fillerA, fillerB, fillerC, fillerB, fillerA, fillerB, fillerC
        };
        for (int i = 0; i < Math.min(pattern.length, size); i++) {
            gui.setItem(i, pattern[i]);
        }

        for (String languageKey : LanguageManager.getSupportedLanguageKeys()) {
            String basePath = "messages.gui.Language.languages." + languageKey;
            int slot = parseInt(LanguageManager.getMessage(player, basePath + ".slot"), -1);
            if (slot < 0 || slot >= size) {
                continue;
            }
            String texture = LanguageManager.getMessage(player, basePath + ".texture");
            String name = LanguageManager.getMessage(player, basePath + ".name");
            List<String> lore = LanguageManager.getMessageList(player, basePath + ".lore");
            gui.setItem(slot, createLanguageHead(texture, name, lore, languageKey));
        }

        int closeSlot = parseInt(LanguageManager.getMessage(player, "messages.gui.Language.buttons.Close.slot"), -1);
        if (closeSlot >= 0 && closeSlot < size) {
            gui.setItem(closeSlot, createButton(player, "messages.gui.Language.buttons.Close"));
        }

        player.openInventory(gui);
    }

    private static ItemStack createGlass(Player player, String path) {
        Material material = Material.valueOf(LanguageManager.getMessage(player, path));
        return createItem(material, "§7", List.of());
    }

    private static ItemStack createButton(Player player, String path) {
        Material material = Material.valueOf(LanguageManager.getMessage(player, path + ".material"));
        String name = LanguageManager.getMessage(player, path + ".name");
        List<String> lore = LanguageManager.getMessageList(player, path + ".lore");
        return createItem(material, name, lore);
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

    private static ItemStack createLanguageHead(String texture, String name, List<String> lore, String languageKey) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        if (texture != null && !texture.isBlank() && !texture.equalsIgnoreCase("none")) {
            applyTexture(meta, texture);
        }
        meta.getPersistentDataContainer().set(LANGUAGE_KEY, PersistentDataType.STRING, languageKey);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyTexture(SkullMeta meta, String texture) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", texture));
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // ignored
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
