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

public class SessionPremiumCheck {

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

    public static boolean handlePremiumRegister(Player player) {
        if (!PlayerRegisterManager.isPlayerRegistered(player)) {
                if (isPlayerPremium(player.getName())) {
                    PasswordSecurity.encodeAsync(MainSpigot.getInstance(), "none", encodedPassword -> {
                        PlayerDataSave.savePlayerData(player, encodedPassword);
                        LanguageAutoDetect.applyAutoDetectOnFirstJoin(player);
                        PlayerDataSave.setPlayerSession(player.getName(), true);
                        PlayerRestrictions.unblockPlayer(player);
                        LoginMessagesManager.PremiumRegisterTitle(player);
                    });
                    return true;
                }
        }
        return false;
    }
}
