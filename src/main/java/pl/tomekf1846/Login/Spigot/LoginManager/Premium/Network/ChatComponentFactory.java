package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

public final class ChatComponentFactory {

    private ChatComponentFactory() {
    }

    public static Object fromText(String text) {
        MinecraftVersionResolver version = MinecraftVersionResolver.get();
        String message = Objects.toString(text, "");

        if (version.isMojangMapped()) {
            Object literal = tryModernLiteral(message);
            if (literal != null) {
                return literal;
            }
        } else {
            Object legacyText = tryLegacyTextComponent(message, version.getLegacyBasePackage());
            if (legacyText != null) {
                return legacyText;
            }
        }

        Object viaSerializer = trySerializer(message, version.getChatComponentSerializerClasses());
        if (viaSerializer != null) {
            return viaSerializer;
        }

        if (!version.isMojangMapped()) {
            Object legacyFallback = tryLegacyTextComponent(message, version.getLegacyBasePackage());
            if (legacyFallback != null) {
                return legacyFallback;
            }
        }

        return null;
    }

    private static Object tryModernLiteral(String text) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            return literal.invoke(null, text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object tryLegacyTextComponent(String text, String legacyBase) {
        if (legacyBase == null) {
            return null;
        }
        try {
            Class<?> chatComponentText = Class.forName(legacyBase + ".ChatComponentText");
            Constructor<?> ctor = chatComponentText.getConstructor(String.class);
            return ctor.newInstance(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object trySerializer(String text, String[] serializerClassNames) {
        if (serializerClassNames == null) {
            return null;
        }
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        String json = "{\"text\":\"" + escaped + "\"}";

        for (String className : serializerClassNames) {
            if (className == null || className.isEmpty()) {
                continue;
            }
            try {
                Class<?> serializer = Class.forName(className);
                Method method = resolveSerializerMethod(serializer);
                if (method == null) {
                    continue;
                }
                return method.invoke(null, json);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Method resolveSerializerMethod(Class<?> serializer) {
        for (String methodName : new String[]{"fromJson", "a", "deserialize"}) {
            try {
                Method method = serializer.getMethod(methodName, String.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
