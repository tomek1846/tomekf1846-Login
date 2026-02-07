package pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.LanguageAutoDetect;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager;
import pl.tomekf1846.Login.Spigot.MainSpigot;
import pl.tomekf1846.Login.Spigot.Security.PasswordSecurity;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class SessionPremiumCheck {

    public static void checkPremiumAsync(String nickname, Consumer<Boolean> callback) {
        MainSpigot plugin = MainSpigot.getInstance();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean premium = isPlayerPremium(nickname);
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(premium));
        });
    }

    public static void registerPremiumPlayer(Player player) {
        if (PlayerRegisterManager.isPlayerRegistered(player)) {
            return;
        }
        PasswordSecurity.encodeAsync(MainSpigot.getInstance(), "none", encodedPassword -> {
            PlayerDataSave.savePlayerData(player, encodedPassword);
            LanguageAutoDetect.applyAutoDetectOnFirstJoin(player);
            PlayerDataSave.setPlayerSession(player.getName(), true);
            PlayerRestrictions.unblockPlayer(player);
            LoginMessagesManager.PremiumRegisterTitle(player);
        });
    }

    public static boolean isPlayerPremium(String nickname) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + nickname);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
            String name = response.get("name").getAsString();
            return name != null && !name.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
