package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import java.lang.reflect.Method;
import java.util.Objects;

public final class ChatComponentFactory {

    private ChatComponentFactory() {
    }

    public static Object fromText(String text) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            return literal.invoke(null, text);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                Class<?> serializer = Class.forName("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
                Method a = serializer.getMethod("a", String.class);
                String json = Objects.toString(text, "")
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");
                return a.invoke(null, "{\"text\":\"" + json + "\"}");
            } catch (Exception ignored) {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
}