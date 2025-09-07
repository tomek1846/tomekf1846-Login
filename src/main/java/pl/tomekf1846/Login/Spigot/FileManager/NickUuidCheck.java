package pl.tomekf1846.Login.Spigot.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class NickUuidCheck {
    private static final String PLAYER_DATA_FOLDER = "plugins/tomekf1846-Login/Data/";

    public static UUID getUUIDFromNick(String nick) {
        File folder = new File(PLAYER_DATA_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return null;
        }

        for (File file : files) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                String foundNick = null;
                String foundUUID = null;

                for (String line : lines) {
                    if (line.startsWith("Nick: ")) {
                        foundNick = line.substring(6).trim();
                    } else if (line.startsWith("Player-UUID: ")) {
                        foundUUID = line.substring(13).trim();
                    }

                    if (foundNick != null && foundUUID != null && foundNick.equalsIgnoreCase(nick)) {
                        return UUID.fromString(foundUUID);
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
