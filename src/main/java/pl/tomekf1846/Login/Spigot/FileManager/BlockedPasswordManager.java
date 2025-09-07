package pl.tomekf1846.Login.Spigot.FileManager;

import java.io.*;

public class BlockedPasswordManager {

    public static void copyBlockedPasswordFile(File dataFolder) {
        File blockedPasswordFile = new File(dataFolder, "blocked-passwords.yml");

        if (!blockedPasswordFile.exists()) {
            try {
                dataFolder.mkdirs();

                try (InputStream in = BlockedPasswordManager.class.getClassLoader().getResourceAsStream("blocked-passwords.yml");
                     OutputStream out = new FileOutputStream(blockedPasswordFile)) {

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error while copying Blocked-Password.yml file!");
                e.printStackTrace();
            }
        }
    }
}
