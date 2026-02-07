package pl.tomekf1846.Login.Spigot.GUI;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public final class GuiChatHelper {
    private GuiChatHelper() {
    }

    public static void sendMessageWithCancelButton(Player player, String message) {
        if (player == null) {
            return;
        }
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        String cancelText = LanguageManager.getMessage(player, "messages.gui.cancel-button.text");
        String cancelHover = LanguageManager.getMessage(player, "messages.gui.cancel-button.hover");

        TextComponent base = new TextComponent(colorize(prefix + message + " "));
        TextComponent cancelButton = new TextComponent(colorize(cancelText));
        cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/alogin cancel"));
        if (cancelHover != null && !cancelHover.isBlank()) {
            cancelButton.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(colorize(cancelHover)).create()
            ));
        }
        base.addExtra(cancelButton);
        player.spigot().sendMessage(base);
    }

    private static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
