package pl.tomekf1846.Login.Spigot.FileManager;

import java.util.UUID;

public class NickUuidCheck {
    public static UUID getUUIDFromNick(String nick) {
        return PlayerDataSave.findUUIDByNick(nick);
    }
}
