package pl.tomekf1846.Login.Spigot.LoginManager.Session.Premium;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.LoginMessagesManager;
import pl.tomekf1846.Login.Spigot.LoginManager.Other.PlayerRestrictions;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Network.MinecraftVersionResolver;
import pl.tomekf1846.Login.Spigot.LoginManager.Register.PlayerRegisterManager;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SessionPremiumCheck {

    public static boolean isPlayerPremium(String nickname) {
        if (MinecraftVersionResolver.get().isAtLeast(1, 20, 5)) {
            try {
                PlayerProfile profile = Bukkit.createProfile(nickname);
                return profile.completeFromCache(true) || profile.complete();
            } catch (Exception e) {
                return false;
            }
        }
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

    public static boolean handlePremiumRegister(Player player) {
        if (!PlayerRegisterManager.isPlayerRegistered(player)) {
            if (isPlayerPremium(player.getName())) {
                    PlayerDataSave.savePlayerData(player, "none");
                    PlayerDataSave.setPlayerSession(player.getName(), true);
                    PlayerRestrictions.unblockPlayer(player);
                    LoginMessagesManager.PremiumRegisterTitle(player);
                    return true;
                }
        }
        return false;
    }
}
