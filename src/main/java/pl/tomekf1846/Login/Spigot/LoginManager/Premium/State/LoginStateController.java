package pl.tomekf1846.Login.Spigot.LoginManager.Premium.State;

import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LoginStateController {

    private LoginStateController() {
    }

    public static void setReadyToAccept(Object loginHandler) {
        if (loginHandler == null) {
            return;
        }
        try {
            Field stateField = resolveStateField(loginHandler);
            if (stateField == null) {
                throw new IllegalStateException("Nie znaleziono pola state w ServerLoginPacketListenerImpl");
            }

            stateField.setAccessible(true);
            Class<?> stateClass = stateField.getType();
            Object ready = resolveReadyState(stateClass);

            if (!tryInvokeStateSetter(loginHandler, stateClass, ready)) {
                stateField.set(loginHandler, ready);
            } else {
                try {
                    stateField.set(loginHandler, ready);
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field resolveStateField(Object loginHandler) {
        Field candidate = null;
        for (Field field : loginHandler.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();
            String typeName = type.getName();
            if (type.isEnum() && enumContainsReadyConstant(type)) {
                field.setAccessible(true);
                return field;
            }
            if (typeName.contains("ServerLoginPacketListenerImpl$State")
                    || typeName.contains("LoginListener")
                    || typeName.contains("LoginState")) {
                candidate = field;
            }
        }

        if (candidate != null) {
            candidate.setAccessible(true);
            if (candidate.getType().isEnum() && enumContainsReadyConstant(candidate.getType())) {
                return candidate;
            }
        }

        try {
            Field state = loginHandler.getClass().getDeclaredField("state");
            state.setAccessible(true);
            if (!state.getType().isEnum() || enumContainsReadyConstant(state.getType())) {
                return state;
            }
        } catch (NoSuchFieldException ignored) {
        }

        for (Field field : loginHandler.getClass().getDeclaredFields()) {
            if (field.getType().isEnum() && enumContainsReadyConstant(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static boolean enumContainsReadyConstant(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return false;
        }
        for (Object constant : constants) {
            if (!(constant instanceof Enum<?> enumConst)) {
                continue;
            }
            String name = enumConst.name();
            if (name.contains("READY") || name.contains("ACCEPT") || name.contains("JOIN")) {
                return true;
            }
        }
        return false;
    }

    private static Object resolveReadyState(Class<?> stateClass) {
        Object[] constants = stateClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalStateException("Brak stałych enum dla stanu logowania");
        }

        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConst && "READY_TO_ACCEPT".equals(enumConst.name())) {
                return constant;
            }
        }

        Object ready = findStateByNameFragments(constants, "READY", "ACCEPT");
        if (ready == null) {
            ready = findStateByNameFragments(constants, "ACCEPT");
        }
        if (ready == null) {
            ready = findStateByNameFragments(constants, "JOIN");
        }
        if (ready == null) {
            ready = constants[constants.length - 1];
        }

        if (ready instanceof Enum<?>) {
            return ready;
        }

        throw new IllegalStateException("Nie znaleziono odpowiedniego stanu READY/ACCEPT w ServerLoginPacketListenerImpl");
    }

    private static Object findStateByNameFragments(Object[] constants, String... fragments) {
        for (Object constant : constants) {
            if (!(constant instanceof Enum<?> enumConst)) {
                continue;
            }
            String name = enumConst.name();
            boolean matches = true;
            for (String fragment : fragments) {
                if (!name.contains(fragment)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return constant;
            }
        }
        return null;
    }

    private static boolean tryInvokeStateSetter(Object loginHandler, Class<?> stateClass, Object ready) {
        Method[] methods = loginHandler.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterCount() != 1) {
                continue;
            }

            Class<?> paramType = method.getParameterTypes()[0];
            if (!paramType.isAssignableFrom(stateClass) && !stateClass.isAssignableFrom(paramType)) {
                continue;
            }

            if (method.getReturnType() != Void.TYPE && !paramType.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            String name = method.getName().toLowerCase();
            if (!(name.contains("state") || name.contains("login") || name.contains("switch"))) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(loginHandler, ready);
                return true;
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    public static void setLoginGameProfile(Object loginHandler, MojangProfile profile) {
        if (loginHandler == null || profile == null) {
            return;
        }
        try {
            Class<?> gpClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> gpCtor = gpClass.getConstructor(UUID.class, String.class);
            Object gp = gpCtor.newInstance(profile.uuid, profile.name);

            Method getProps = gpClass.getMethod("getProperties");
            Object propMap = getProps.invoke(gp);

            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propCtor = propClass.getConstructor(String.class, String.class);
            Constructor<?> propCtorSig = null;
            try {
                propCtorSig = propClass.getConstructor(String.class, String.class, String.class);
            } catch (NoSuchMethodException ignored) {
            }

            List<Map<String, String>> props = profile.properties;
            if (props != null) {
                for (Map<String, String> m : props) {
                    String pname = m.get("name");
                    String value = m.get("value");
                    String signature = m.get("signature");
                    Object prop;
                    if (signature != null && propCtorSig != null) {
                        prop = propCtorSig.newInstance(pname, value, signature);
                    } else {
                        prop = propCtor.newInstance(pname, value);
                    }
                    propMap.getClass().getMethod("put", Object.class, Object.class).invoke(propMap, pname, prop);
                }
            }

            setProfileOnHandler(loginHandler, gpClass, gp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setProfileOnHandler(Object loginHandler, Class<?> gpClass, Object gp) throws IllegalAccessException {
        for (Field f : loginHandler.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                if (f.getType().getName().equals(gpClass.getName())) {
                    f.set(loginHandler, gp);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            Field f = loginHandler.getClass().getDeclaredField("gameProfile");
            f.setAccessible(true);
            f.set(loginHandler, gp);
            return;
        } catch (NoSuchFieldException ignored) {
        }
        try {
            Field f = loginHandler.getClass().getDeclaredField("profile");
            f.setAccessible(true);
            f.set(loginHandler, gp);
        } catch (NoSuchFieldException ignored) {
        }
    }
}
