package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.channel.Channel;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumSession;

import java.util.logging.Logger;

public class EncryptionRequestSender {

    private final ProtocolManager protocolManager;
    private final Logger logger;

    public EncryptionRequestSender(Plugin plugin, ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
        this.logger = plugin.getLogger();
    }

    public void send(PacketEvent event, Channel channel, PremiumSession session) throws Exception {
        PacketContainer request = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
        request.getStrings().write(0, session.serverId);
        request.getByteArrays().write(0, session.keyPair.getPublic().getEncoded());
        request.getByteArrays().write(1, session.verifyToken);

        boolean delivered = tryProtocolLib(event, request);
        if (!delivered && channel != null) {
            delivered = tryChannel(channel, request);
        }

        if (!delivered) {
            throw new IllegalStateException("Unable to deliver ENCRYPTION_BEGIN to client");
        }
    }

    private boolean tryProtocolLib(PacketEvent event, PacketContainer packet) {
        try {
            if (event.getPlayer() != null) {
                protocolManager.sendServerPacket(event.getPlayer(), packet);
                return true;
            }
        } catch (Exception ex) {
            logger.warning("[PremiumLogin] Failed to send ENCRYPTION_BEGIN via ProtocolLib: " + ex.getMessage());
        }
        return false;
    }

    private boolean tryChannel(Channel channel, PacketContainer packet) {
        try {
            Object handle = packet.getHandle();
            if (handle == null) {
                return false;
            }
            channel.writeAndFlush(handle);
            return true;
        } catch (Exception ex) {
            logger.warning("[PremiumLogin] Failed to send ENCRYPTION_BEGIN via Netty channel: " + ex.getMessage());
            return false;
        }
    }
}