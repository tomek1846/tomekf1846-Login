package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class LoginDisconnectHelper {

    private final ProtocolManager protocolManager;
    private final ConnectionResolver connectionResolver;
    private final Logger logger;
    private final MinecraftVersionResolver versionResolver;

    public LoginDisconnectHelper(Plugin plugin, ProtocolManager protocolManager, ConnectionResolver connectionResolver) {
        this.protocolManager = protocolManager;
        this.connectionResolver = connectionResolver;
        this.logger = plugin.getLogger();
        this.versionResolver = MinecraftVersionResolver.get();
    }

    public void disconnect(PacketEvent event, Object connection, String message) {
        if (sendDisconnectPacket(event, message)) {
            return;
        }

        Object resolved = connection != null ? connection : resolveConnection(event);
        if (resolved == null) {
            logger.warning("[PremiumLogin] Nie można rozłączyć połączenia (brak referencji connection). Msg: " + message);
            return;
        }

        if (invokeDisconnectOnLoginHandler(resolved, message)) {
            return;
        }

        if (invokeDisconnect(resolved, message)) {
            return;
        }

        if (closeChannel(resolved)) {
            return;
        }

        logger.warning("[PremiumLogin] Nie udało się rozłączyć gracza mimo prób fallback. Msg: " + message);
    }

    private boolean sendDisconnectPacket(PacketEvent event, String message) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Login.Server.DISCONNECT);
            packet.getChatComponents().write(0, WrappedChatComponent.fromText(message));
            Player player = event.getPlayer();
            if (player != null) {
                protocolManager.sendServerPacket(player, packet);
                return true;
            }
        } catch (Exception ex) {
            logger.warning("[PremiumLogin] Failed to send disconnect packet: " + ex.getMessage());
        }
        return false;
    }

    private Object resolveConnection(PacketEvent event) {
        try {
            return connectionResolver.findConnectionFor(event);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean invokeDisconnectOnLoginHandler(Object connection, String message) {
        Object loginHandler = ConnectionResolver.extractFieldType(connection, versionResolver.getLoginHandlerClassPrefixes());
        if (loginHandler != null) {
            return invokeDisconnect(loginHandler, message);
        }
        return false;
    }

    private boolean invokeDisconnect(Object target, String message) {
        if (target == null) {
            return false;
        }

        Object component = ChatComponentFactory.fromText(message);
        if (component == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals("disconnect") || method.getParameterCount() != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isInstance(component)) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(target, component);
                return true;
            } catch (Exception ex) {
                logger.warning("[PremiumLogin] disconnect() invocation failed: " + ex.getMessage());
                return false;
            }
        }
        return false;
    }

    private boolean closeChannel(Object connection) {
        try {
            Channel channel = connectionResolver.findChannel(connection);
            if (channel == null) {
                return false;
            }
            channel.close();
            return true;
        } catch (Exception ex) {
            logger.warning("[PremiumLogin] Channel close failed: " + ex.getMessage());
            return false;
        }
    }
}