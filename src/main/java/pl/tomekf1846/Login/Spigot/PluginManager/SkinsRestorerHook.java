package pl.tomekf1846.Login.Spigot.PluginManager;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.Locale;

public final class SkinsRestorerHook {
    private SkinsRestorerHook() {
    }

    public static boolean applySkullTexture(ItemStack head, SkullMeta meta, String textureValue) {
        if (!isAvailable() || textureValue == null || textureValue.isBlank()) {
            return false;
        }
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Method getMethod = providerClass.getMethod("get");
            Object api = getMethod.invoke(null);
            return applyViaApi(api, head, meta, textureValue);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean applySkinByName(ItemStack head, SkullMeta meta, String skinName) {
        if (!isAvailable() || skinName == null || skinName.isBlank()) {
            return false;
        }
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Method getMethod = providerClass.getMethod("get");
            Object api = getMethod.invoke(null);
            return applyViaApi(api, head, meta, skinName);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean applyViaApi(Object api, ItemStack head, SkullMeta meta, String textureValue)
            throws ReflectiveOperationException {
        for (Method method : api.getClass().getMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("apply")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[1] == String.class) {
                if (params[0].isAssignableFrom(ItemStack.class)) {
                    method.invoke(api, head, textureValue);
                    return true;
                }
                if (params[0].isAssignableFrom(SkullMeta.class)) {
                    method.invoke(api, meta, textureValue);
                    return true;
                }
            }
            if (params.length == 2 && params[0] == String.class) {
                if (params[1].isAssignableFrom(ItemStack.class)) {
                    method.invoke(api, textureValue, head);
                    return true;
                }
                if (params[1].isAssignableFrom(SkullMeta.class)) {
                    method.invoke(api, textureValue, meta);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
    }
}
