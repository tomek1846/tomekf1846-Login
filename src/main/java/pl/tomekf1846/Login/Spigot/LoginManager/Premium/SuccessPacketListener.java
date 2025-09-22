package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SuccessPacketListener extends PacketAdapter {

    private final PremiumLoginListener loginListener;

    public SuccessPacketListener(Plugin plugin, PremiumLoginListener loginListener) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Login.Server.SUCCESS);
        this.loginListener = loginListener;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        MojangProfile profile = loginListener.consumeVerifiedProfile(event);
        if (profile == null) return;

        try {
            UUID uuid = profile.uuid;
            String name = profile.name;

            WrappedGameProfile wrapped = new WrappedGameProfile(uuid, name);

            List<Map<String, String>> props = profile.properties;
            if (props != null) {
                for (Map<String, String> p : props) {
                    String pname = p.get("name");
                    String value = p.get("value");
                    String signature = p.get("signature");
                    if (signature != null) {
                        wrapped.getProperties().put(pname, new WrappedSignedProperty(pname, value, signature));
                    } else {
                        wrapped.getProperties().put(pname, new WrappedSignedProperty(pname, value, null));
                    }
                }
            }

            event.getPacket().getGameProfiles().write(0, wrapped);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
