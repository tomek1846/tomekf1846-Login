package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangAuthClient;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.ConnectionResolver;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.EncryptionRequestSender;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.LoginDisconnectHelper;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.LoginFinalizer;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumVerifiedProfileStore;

public class PremiumLoginListener extends PacketAdapter {

    private final PremiumHandshakeManager handshakeManager;

    public PremiumLoginListener(Plugin plugin, ProtocolManager protocolManager, PremiumVerifiedProfileStore profileStore) {
        super(plugin, ListenerPriority.HIGHEST,
                PacketType.Login.Client.START,
                PacketType.Login.Client.ENCRYPTION_BEGIN);

        ConnectionResolver connectionResolver = new ConnectionResolver(plugin);
        EncryptionRequestSender requestSender = new EncryptionRequestSender(plugin, protocolManager);
        MojangAuthClient authClient = new MojangAuthClient(plugin);
        LoginDisconnectHelper disconnectHelper = new LoginDisconnectHelper(plugin, protocolManager, connectionResolver);
        LoginFinalizer loginFinalizer = new LoginFinalizer(plugin, connectionResolver);
        PremiumEligibilityChecker eligibilityChecker = new PremiumEligibilityChecker(plugin);

        this.handshakeManager = new PremiumHandshakeManager(
                plugin,
                requestSender,
                authClient,
                connectionResolver,
                disconnectHelper,
                loginFinalizer,
                eligibilityChecker,
                profileStore
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.START) {
            handshakeManager.handleLoginStart(event);
        } else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_BEGIN) {
            handshakeManager.handleEncryptionResponse(event);
        }
    }

    public MojangProfile consumeVerifiedProfile(PacketEvent event) {
        return handshakeManager.consumeVerifiedProfile(event);
    }

    public void clearSessions() {
        handshakeManager.clearSessions();
    }

    public void clearPlayerCache(org.bukkit.entity.Player player) {
        handshakeManager.clearPlayerCache(player);
    }
}
