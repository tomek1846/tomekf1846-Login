package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.MinecraftVersionResolver;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumVerifiedProfileStore;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class PremiumProfileListener implements Listener {

    private final PremiumVerifiedProfileStore profileStore;
    private final MinecraftVersionResolver versionResolver;
    private final Logger logger;

    public PremiumProfileListener(Plugin plugin, PremiumVerifiedProfileStore profileStore) {
        this.profileStore = profileStore;
        this.versionResolver = MinecraftVersionResolver.get();
        this.logger = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!versionResolver.isAtLeast(1, 20, 5)) {
            return;
        }

        String username = event.getName();
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : null;
        MojangProfile profile = profileStore.consume(username, ip);
        if (profile == null) {
            return;
        }

        PlayerProfile paperProfile = Bukkit.createProfileExact(profile.uuid, profile.name);
        Set<ProfileProperty> properties = new HashSet<>();
        List<Map<String, String>> props = profile.properties;
        if (props != null) {
            for (Map<String, String> property : props) {
                String propertyName = property.get("name");
                String value = property.get("value");
                String signature = property.get("signature");
                if (propertyName == null || value == null) {
                    continue;
                }
                if (signature != null) {
                    properties.add(new ProfileProperty(propertyName, value, signature));
                } else {
                    properties.add(new ProfileProperty(propertyName, value));
                }
            }
        }

        if (!properties.isEmpty()) {
            paperProfile.setProperties(properties);
        }

        event.setPlayerProfile(paperProfile);
        logger.info("[PremiumLogin] Paper profile applied for username=" + profile.name);
    }
}
