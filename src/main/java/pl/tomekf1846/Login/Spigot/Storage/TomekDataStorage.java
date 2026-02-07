package pl.tomekf1846.Login.Spigot.Storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TomekDataStorage extends YamlPlayerDataStorage {
    public TomekDataStorage(JavaPlugin plugin) {
        super(plugin, new File(plugin.getDataFolder(), "Data"));
    }
}
