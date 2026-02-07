package pl.tomekf1846.Login.Spigot.GUI;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageManager;

public final class ChatCancelButton {
    private ChatCancelButton() {
    }

    public static void send(Player player) {
        if (player == null) {
            return;
        }
        String prefix = LanguageManager.getMessage(player, "messages.prefix.main-prefix");
        String text = prefix + LanguageManager.getMessage(player, "messages.gui.cancel-button.text");
        String hover = LanguageManager.getMessage(player, "messages.gui.cancel-button.hover");
        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/logincancel"));
        component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', hover)).create()
        ));
        player.spigot().sendMessage(component);
    }
}
