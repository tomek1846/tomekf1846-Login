package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.MinecraftVersionResolver;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SuccessPacketListener extends PacketAdapter {

    private final PremiumLoginListener loginListener;
    private final MinecraftVersionResolver versionResolver;

    public SuccessPacketListener(Plugin plugin, PremiumLoginListener loginListener) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Login.Server.SUCCESS);
        this.loginListener = loginListener;
        this.versionResolver = MinecraftVersionResolver.get();
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (versionResolver.isAtLeast(1, 20, 5)) {
            return;
        }
        MojangProfile profile = loginListener.consumeVerifiedProfile(event);
        if (profile == null) {
            return;
        }

        try {
            UUID uuid = profile.uuid;
            String name = profile.name;

            WrappedGameProfile wrapped = new WrappedGameProfile(uuid, name);

            List<Map<String, String>> props = profile.properties;
            if (props != null) {
                for (Map<String, String> property : props) {
                    String propertyName = property.get("name");
                    String value = property.get("value");
                    String signature = property.get("signature");
                    if (signature != null) {
                        wrapped.getProperties().put(propertyName, new WrappedSignedProperty(propertyName, value, signature));
                    } else {
                        wrapped.getProperties().put(propertyName, new WrappedSignedProperty(propertyName, value, null));
                    }
                }
            }

            event.getPacket().getGameProfiles().write(0, wrapped);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
