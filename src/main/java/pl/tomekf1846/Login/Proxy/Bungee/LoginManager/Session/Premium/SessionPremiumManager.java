package pl.tomekf1846.Login.Proxy.Bungee.LoginManager.Session.Premium;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SessionPremiumManager implements Listener {

    private final Set<String> playersRequiringAuth = new HashSet<>();

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.getServer() != null) {
            player.getServer().sendData("BungeeCord", player.getName().getBytes());
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("BungeeCord")) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String playerName = in.readUTF();
            boolean requireAuth = in.readBoolean();

            if (requireAuth) {
                playersRequiringAuth.add(playerName);
                ProxiedPlayer player = event.getReceiver() instanceof ProxiedPlayer ? (ProxiedPlayer) event.getReceiver() : null;
                if (player != null && player.getName().equals(playerName)) {
                    player.disconnect("Musisz połączyć się przez konto Mojang.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
