package pl.tomekf1846.Login.Spigot.LoginManager.Premium.State;

import io.netty.util.AttributeKey;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Auth.MojangProfile;
import pl.tomekf1846.Login.Spigot.LoginManager.Premium.Session.PremiumSession;

public final class PremiumConnectionKeys {

    private PremiumConnectionKeys() {
    }

    public static final AttributeKey<PremiumSession> PREMIUM_SESSION = AttributeKey.valueOf("login-premium-session");
    public static final AttributeKey<MojangProfile> VERIFIED_PROFILE = AttributeKey.valueOf("login-premium-verified-profile");
    public static final AttributeKey<Boolean> CLEANUP_ATTACHED = AttributeKey.valueOf("login-premium-cleanup-attached");
    public static final AttributeKey<Object> LOGIN_CONNECTION = AttributeKey.valueOf("login-premium-connection");
}