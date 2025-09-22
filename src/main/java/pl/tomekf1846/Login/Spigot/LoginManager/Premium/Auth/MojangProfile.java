package pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MojangProfile {
    public final UUID uuid;
    public final String name;
    public final List<Map<String, String>> properties;

    public MojangProfile(UUID uuid, String name, List<Map<String, String>> properties) {
        this.uuid = uuid;
        this.name = name;
        this.properties = properties;
    }
}
