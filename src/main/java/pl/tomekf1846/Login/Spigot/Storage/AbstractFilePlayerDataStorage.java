package pl.tomekf1846.Login.Spigot.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;

public abstract class AbstractFilePlayerDataStorage implements PlayerDataStorage {
    protected final JavaPlugin plugin;
    protected final File baseDirectory;
    protected final Gson gson;

    protected AbstractFilePlayerDataStorage(JavaPlugin plugin, File baseDirectory) {
        this.plugin = plugin;
        this.baseDirectory = baseDirectory;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectory();
    }

    protected void ensureDirectory() {
        if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
            plugin.getLogger().warning("Unable to create data directory: " + baseDirectory.getAbsolutePath());
        }
    }

    protected File fileFor(UUID uuid, String extension) {
        return new File(baseDirectory, uuid.toString() + extension);
    }

    protected void safeReplace(Path tempFile, Path targetFile) throws IOException {
        try {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    protected void logSaveFailure(File target, Exception ex) {
        plugin.getLogger().log(Level.WARNING, "Failed saving player data: " + target.getAbsolutePath(), ex);
    }

    @Override
    public void close() {
        // No resources to close for file storage.
    }
}
