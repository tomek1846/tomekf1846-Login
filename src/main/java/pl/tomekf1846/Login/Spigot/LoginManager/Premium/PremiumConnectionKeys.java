package pl.tomekf1846.Login.Spigot.LoginManager.Premium;

import io.netty.util.AttributeKey;

final class PremiumConnectionKeys {

    private PremiumConnectionKeys() {
    }

    static final AttributeKey<PremiumSession> PREMIUM_SESSION = AttributeKey.valueOf("login-premium-session");
    static final AttributeKey<MojangProfile> VERIFIED_PROFILE = AttributeKey.valueOf("login-premium-verified-profile");
    static final AttributeKey<Boolean> CLEANUP_ATTACHED = AttributeKey.valueOf("login-premium-cleanup-attached");
    static final AttributeKey<Object> LOGIN_CONNECTION = AttributeKey.valueOf("login-premium-connection");
}