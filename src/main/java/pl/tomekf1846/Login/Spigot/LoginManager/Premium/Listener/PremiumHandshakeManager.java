package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Listener;

import com.comphenix.protocol.events.PacketEvent;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangAuthClient;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.ServerHashGenerator;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.ConnectionResolver;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.EncryptionRequestSender;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.LoginDisconnectHelper;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.LoginFinalizer;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumSession;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumSessionFactory;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumSessionRegistry;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.State.PremiumConnectionKeys;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.logging.Logger;

public class PremiumHandshakeManager {

    private final PremiumSessionFactory sessionFactory = new PremiumSessionFactory();
    private final PremiumSessionRegistry sessionRegistry = new PremiumSessionRegistry();
    private final ConnectionResolver connectionResolver;
    private final EncryptionRequestSender encryptionRequestSender;
    private final MojangAuthClient mojangAuthClient;
    private final LoginDisconnectHelper disconnectHelper;
    private final LoginFinalizer loginFinalizer;
    private final PremiumEligibilityChecker eligibilityChecker;
    private final Logger logger;

    public PremiumHandshakeManager(Plugin plugin,
                                   EncryptionRequestSender encryptionRequestSender,
                                   MojangAuthClient mojangAuthClient,
                                   ConnectionResolver connectionResolver,
                                   LoginDisconnectHelper disconnectHelper,
                                   LoginFinalizer loginFinalizer,
                                   PremiumEligibilityChecker eligibilityChecker) {
        this.encryptionRequestSender = encryptionRequestSender;
        this.mojangAuthClient = mojangAuthClient;
        this.connectionResolver = connectionResolver;
        this.disconnectHelper = disconnectHelper;
        this.loginFinalizer = loginFinalizer;
        this.eligibilityChecker = eligibilityChecker;
        this.logger = plugin.getLogger();
    }

    public void handleLoginStart(PacketEvent event) {
        String username = event.getPacket().getStrings().read(0);
        if (!eligibilityChecker.shouldHandle(username)) {
            return;
        }

        try {
            PremiumSession session = sessionFactory.createSession(username);
            sessionRegistry.register(session);

            ConnectionContext context = resolveContext(event);
            Channel channel = context.channel();
            Object connection = context.connection();

            if (channel != null) {
                channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(session);
                if (connection != null) {
                    channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(connection);
                }
                ensureCleanup(channel);
                logger.info("[PremiumLogin] START username=" + username + " bound premium session to channel "
                        + channel.remoteAddress() + " (key=" + context.key() + ")");
            } else {
                logger.warning("[PremiumLogin] START username=" + username + " - unable to resolve Netty channel (key="
                        + context.key() + "). Falling back to pending session queue.");
            }

            encryptionRequestSender.send(event, channel, session);
        } catch (Exception ex) {
            logger.warning("[PremiumLogin] START handler failed: " + ex.getMessage());
            disconnectHelper.disconnect(event, null, "Failed to verify username!");
        }
    }

    public void handleEncryptionResponse(PacketEvent event) {
        byte[] encShared = event.getPacket().getByteArrays().read(0);
        byte[] encToken = event.getPacket().getByteArrays().read(1);

        ConnectionContext context = resolveContext(event);
        Channel channel = context.channel();
        Object connection = context.connection();

        String username = extractUsername(event, channel);

        PremiumSession session = getSession(channel);
        if (session == null) {
            session = sessionRegistry.claim(username, encToken);
            if (session != null && channel != null) {
                channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(session);
                ensureCleanup(channel);
            }
        }

        if (session == null) {
            logger.warning("[PremiumLogin] No premium session bound to connection " + context.key() + ". Ignoring.");
            event.setCancelled(true);
            disconnectHelper.disconnect(event, connection, "Failed to verify username!");
            if (channel != null) {
                channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).set(null);
            }
            sessionRegistry.discard(username);
            return;
        }

        event.setCancelled(true);

        try {
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, session.keyPair.getPrivate());
            byte[] shared = rsa.doFinal(encShared);
            byte[] token = rsa.doFinal(encToken);

            if (!Arrays.equals(token, session.verifyToken)) {
                logger.warning("[PremiumLogin] Token mismatch for session username=" + session.username + " connection=" + context.key());
                disconnectHelper.disconnect(event, connection, "Failed to verify username! (token)");
                clearSession(channel);
                sessionRegistry.remove(session);
                return;
            }

            SecretKey secretKey = PremiumSessionFactory.sharedSecretToKey(shared);
            session.sharedKey = secretKey;

            if (connection == null) {
                logger.warning("[PremiumLogin] Nie udało się znaleźć Connection (ENCRYPTION_BEGIN).");
                disconnectHelper.disconnect(event, null, "Internal error (connection)");
                clearSession(channel);
                sessionRegistry.remove(session);
                return;
            }

            connectionResolver.invokeSetupEncryption(connection, secretKey);

            String serverHash = ServerHashGenerator.compute(session.serverId, shared, session.keyPair.getPublic());
            String ip = resolveIp(event, context);
            logger.info("[PremiumLogin] Calling sessionserver.hasJoined username=" + session.username + " serverHash=" + serverHash
                    + (ip != null ? " ip=" + ip : ""));

            MojangProfile profile = mojangAuthClient.queryHasJoined(session.username, serverHash, ip);
            if (profile == null) {
                logger.warning("[PremiumLogin] hasJoined==null dla " + session.username + " (hash=" + serverHash + "). Wymagane premium -> rozłączam.");
                disconnectHelper.disconnect(event, connection, "§cTo konto wymaga logowania premium.\n§7Zaloguj się launcherem Mojang/Microsoft i spróbuj ponownie.");
                clearSession(channel);
                sessionRegistry.remove(session);
                return;
            }

            if (channel != null) {
                channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(profile);
            }

            loginFinalizer.apply(connection, profile, session.username);
            clearSession(channel);
        } catch (Exception e) {
            logger.warning("[PremiumLogin] Exception in ENCRYPTION_BEGIN: " + e.getMessage());
            disconnectHelper.disconnect(event, connection, "Failed to verify username!");
            clearSession(channel);
            sessionRegistry.remove(session);
        }
    }

    public MojangProfile consumeVerifiedProfile(PacketEvent event) {
        ConnectionContext context = resolveContext(event);
        Channel channel = context.channel();
        if (channel == null) {
            return null;
        }
        MojangProfile profile = channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).get();
        if (profile != null) {
            channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(null);
        }
        return profile;
    }

    public void clearSessions() {
        sessionRegistry.clear();
    }

    private PremiumSession getSession(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).get();
    }

    private void clearSession(Channel channel) {
        if (channel == null) {
            return;
        }
        PremiumSession session = channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).getAndSet(null);
        if (session != null) {
            sessionRegistry.remove(session);
        }
    }

    private void ensureCleanup(Channel channel) {
        if (channel == null) {
            return;
        }
        Boolean attached = channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).get();
        if (Boolean.TRUE.equals(attached)) {
            return;
        }
        channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).set(Boolean.TRUE);
        channel.closeFuture().addListener(future -> clearChannelState(channel));
    }

    private void clearChannelState(Channel channel) {
        if (channel == null) {
            return;
        }
        PremiumSession session = channel.attr(PremiumConnectionKeys.PREMIUM_SESSION).getAndSet(null);
        if (session != null) {
            sessionRegistry.remove(session);
        }
        channel.attr(PremiumConnectionKeys.VERIFIED_PROFILE).set(null);
        channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(null);
        channel.attr(PremiumConnectionKeys.CLEANUP_ATTACHED).set(null);
    }

    private ConnectionContext resolveContext(PacketEvent event) {
        Object connection = null;
        Channel channel = null;
        try {
            connection = connectionResolver.findConnectionFor(event);
            channel = connectionResolver.resolveChannel(event, connection);
        } catch (Exception ignored) {
        }

        if (channel != null && connection == null) {
            connection = channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).get();
        }
        if (channel != null && connection != null) {
            channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(connection);
        }

        if (channel != null && (connection == null || connectionResolver.findChannel(connection) != channel)) {
            Object byChannel = connectionResolver.findConnectionByChannel(channel);
            if (byChannel != null) {
                connection = byChannel;
                channel.attr(PremiumConnectionKeys.LOGIN_CONNECTION).set(connection);
            }
        }

        String key = connectionResolver.connKey(event, connection);
        return new ConnectionContext(connection, channel, key);
    }

    private String extractUsername(PacketEvent event, Channel channel) {
        Player player;
        try {
            player = event.getPlayer();
        } catch (Exception ignored) {
            player = null;
        }
        if (player != null) {
            return player.getName();
        }
        PremiumSession session = getSession(channel);
        return session != null ? session.username : null;
    }

    private String resolveIp(PacketEvent event, ConnectionContext context) {
        try {
            Player player = event.getPlayer();
            if (player != null && player.getAddress() != null && player.getAddress().getAddress() != null) {
                return player.getAddress().getAddress().getHostAddress();
            }
        } catch (Exception ignored) {
        }

        Channel channel = context.channel();
        if (channel != null) {
            SocketAddress address = channel.remoteAddress();
            if (address instanceof InetSocketAddress isa) {
                if (isa.getAddress() != null) {
                    return isa.getAddress().getHostAddress();
                }
                return isa.getHostString();
            }
        }

        Object connection = context.connection();
        if (connection != null) {
            try {
                Channel resolved = connectionResolver.findChannel(connection);
                if (resolved != null) {
                    SocketAddress address = resolved.remoteAddress();
                    if (address instanceof InetSocketAddress isa) {
                        if (isa.getAddress() != null) {
                            return isa.getAddress().getHostAddress();
                        }
                        return isa.getHostString();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private record ConnectionContext(Object connection, Channel channel, String key) {
    }
}