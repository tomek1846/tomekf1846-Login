package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network;

import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.State.LoginStateController;

import java.util.logging.Logger;

public class LoginFinalizer {

    private final Plugin plugin;
    private final ConnectionResolver connectionResolver;
    private final Logger logger;

    public LoginFinalizer(Plugin plugin, ConnectionResolver connectionResolver) {
        this.plugin = plugin;
        this.connectionResolver = connectionResolver;
        this.logger = plugin.getLogger();
    }

    public void apply(Object connection, MojangProfile profile, String username) {
        if (connection == null || profile == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Object loginHandler = ConnectionResolver.extractFieldType(connection, "net.minecraft.server.network.ServerLoginPacketListenerImpl");
            if (loginHandler != null) {
                LoginStateController.setLoginGameProfile(loginHandler, profile);
                LoginStateController.setReadyToAccept(loginHandler);
                logger.info("[PremiumLogin] Set GameProfile and READY_TO_ACCEPT for username=" + username);
            } else {
                logger.warning("[PremiumLogin] Nie znaleziono loginHandler (ServerLoginPacketListenerImpl) do ustawienia profilu.");
            }
        });
    }
}