package pl.tomekf1846.Login.Spigot.GUI.PlayerManage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageSettings;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.PluginManager.SkinsRestorerHook;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManageLanguageGui {
    public static final NamespacedKey LANGUAGE_KEY = new NamespacedKey(MainSpigot.getInstance(), "player_manage_language_key");

    public static void openGUI(Player viewer, String targetName) {
        if (!LanguageSettings.isPerPlayerLanguageEnabled()) {
            String prefix = LanguageManager.getMessage(viewer, "messages.prefix.main-prefix");
            viewer.sendMessage(prefix + LanguageManager.getMessage(viewer, "messages.admin-commands.language_command_disabled"));
            return;
        }
        String title = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.name")
                .replace("{player}", targetName);
        String layout = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.layout");
        int size = layout.length();
        if (size == 0 || size % 9 != 0) {
            size = 27;
            layout = "ABCABCABCBLLLLLLLBABCAXCABC";
        }
        Inventory gui = Bukkit.createInventory(null, size, title);

        ItemStack a = createGlass(viewer, "messages.gui.PlayerManage.Language.filling.filling-A");
        ItemStack b = createGlass(viewer, "messages.gui.PlayerManage.Language.filling.filling-B");
        ItemStack c = createGlass(viewer, "messages.gui.PlayerManage.Language.filling.filling-C");
        ItemStack close = createItem(
                Material.valueOf(LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.buttons.Close.material")),
                LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.buttons.Close.name"),
                LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.Language.buttons.Close.lore")
        );
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.getPersistentDataContainer().set(LANGUAGE_KEY, PersistentDataType.STRING, "close");
            close.setItemMeta(closeMeta);
        }

        List<ItemStack> languageItems = buildLanguageItems(viewer, targetName);
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

        PlayerManageState.setTarget(viewer, targetName);
        viewer.openInventory(gui);
    }

    private static ItemStack createGlass(Player viewer, String path) {
        return createItem(Material.valueOf(LanguageManager.getMessage(viewer, path)), "§7", List.of());
    }

    private static List<ItemStack> buildLanguageItems(Player viewer, String targetName) {
        Map<String, LanguageSettings.LanguageOption> options = LanguageSettings.getLanguageOptions();
        List<ItemStack> items = new ArrayList<>();
        UUID targetUuid = PlayerDataSave.findUUIDByNick(targetName);
        String targetLanguage = targetUuid != null ? PlayerDataSave.getPlayerLanguage(targetUuid) : null;
        if (targetLanguage == null || targetLanguage.isBlank()) {
            targetLanguage = MainSpigot.getInstance().getConfig().getString("Main-Settings.Language", "English");
        }
        targetLanguage = LanguageSettings.normalizeLanguage(targetLanguage);

        for (Map.Entry<String, LanguageSettings.LanguageOption> entry : options.entrySet()) {
            String key = entry.getKey();
            LanguageSettings.LanguageOption option = entry.getValue();
            String name = LanguageManager.getMessage(viewer, "messages.gui.PlayerManage.Language.languages." + key + ".name");
            List<String> lore = LanguageManager.getMessageList(viewer, "messages.gui.PlayerManage.Language.languages." + key + ".lore");
            String statusKey = targetLanguage.equals(key)
                    ? "messages.gui.PlayerManage.Language.status.current"
                    : "messages.gui.PlayerManage.Language.status.available";
            String status = LanguageManager.getMessage(viewer, statusKey);
            String languageName = option != null ? option.commandName() : key;
            List<String> resolvedLore = lore.stream()
                    .map(line -> line.replace("{status}", status).replace("{language}", languageName))
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
            boolean applied = false;
            if (texture != null && !texture.isBlank()) {
                applied = applyTexture(meta, texture);
                if (!applied) {
                    applied = SkinsRestorerHook.applySkullTexture(head, meta, texture);
                }
            }
            if (!applied && owner != null && !owner.isBlank()) {
                applied = SkinsRestorerHook.applySkinByName(head, meta, owner);
                if (!applied) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                }
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private static boolean applyTexture(SkullMeta meta, String textureValue) {
        String skinUrl = resolveSkinUrl(textureValue);
        if (skinUrl == null) {
            return false;
        }

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL(skinUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        return true;
    }

    private static String resolveSkinUrl(String textureValue) {
        if (textureValue == null || textureValue.isBlank()) {
            return null;
        }
        String trimmed = textureValue.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return normalizeTextureUrl(trimmed);
        }
        return normalizeTextureUrl(extractSkinUrlFromBase64(trimmed));
    }

    private static String extractSkinUrlFromBase64(String base64Texture) {
        try {
            String cleaned = base64Texture.trim().replace("\n", "").replace("\r", "");
            String json = new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");
            if (textures == null) return null;

            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null) return null;

            if (!skin.has("url")) return null;
            return skin.get("url").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String normalizeTextureUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("http://textures.minecraft.net/texture/")) {
            return url.replace("http://", "https://");
        }
        return url;
    }
}
