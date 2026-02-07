package pl.tomekf1846.Login.Spigot.Api;

import pl.tomekf1846.Login.Spigot.FileManager.PlayerDataSave;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LoginApi {

    public Optional<LoginPlayerData> getPlayerData(UUID uuid) {
        return LoginPlayerData.fromMap(PlayerDataSave.loadPlayerData(uuid));
    }

    public Optional<LoginPlayerData> getPlayerData(String nick) {
        return findPlayerUuid(nick).flatMap(this::getPlayerData);
    }

    public Optional<UUID> findPlayerUuid(String nick) {
        if (nick == null || nick.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PlayerDataSave.findUUIDByNick(nick));
    }

    public Optional<String> getPlayerEmail(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getEmail);
    }

    public Optional<String> getPlayerEmail(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::getEmail);
    }

    public Optional<String> getPlayerLanguage(UUID uuid) {
        String language = PlayerDataSave.getPlayerLanguage(uuid);
        if (language != null && !language.isBlank()) {
            return Optional.of(language);
        }
        return getPlayerData(uuid).map(LoginPlayerData::getLanguage);
    }

    public Optional<String> getPlayerLanguage(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::getLanguage);
    }

    public Optional<String> getPlayerPassword(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getPassword);
    }

    public Optional<String> getPlayerPassword(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::getPassword);
    }

    public Optional<String> getPlayerFirstIp(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getFirstIp);
    }

    public Optional<String> getPlayerLastIp(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getLastIp);
    }

    public Optional<String> getPlayerPremiumUuid(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getPremiumUuid);
    }

    public Optional<String> getPlayerPremiumUuid(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::getPremiumUuid);
    }

    public Optional<String> getPlayerLeaveTime(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::getLeaveTime);
    }

    public Optional<String> getPlayerLeaveTime(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::getLeaveTime);
    }

    public boolean isPlayerPremium(UUID uuid) {
        return getPlayerData(uuid).map(LoginPlayerData::isPremium).orElse(false);
    }

    public boolean isPlayerPremium(String nick) {
        return getPlayerData(nick).map(LoginPlayerData::isPremium).orElse(false);
    }

    public boolean isPlayerRegistered(UUID uuid) {
        return getPlayerData(uuid).isPresent();
    }

    public boolean isPlayerRegistered(String nick) {
        return getPlayerData(nick).isPresent();
    }

    public void setPlayerPassword(UUID uuid, String newPassword) {
        PlayerDataSave.setPlayerPassword(uuid, newPassword);
    }

    public void setPlayerEmail(UUID uuid, String newEmail) {
        PlayerDataSave.setPlayerEmail(uuid, newEmail);
    }

    public void setPlayerLanguage(UUID uuid, String language) {
        PlayerDataSave.setPlayerLanguage(uuid, language);
    }

    public boolean setPlayerSession(String nick, boolean premium) {
        return PlayerDataSave.setPlayerSession(nick, premium);
    }

    public Map<String, String> getRawPlayerData(UUID uuid) {
        return PlayerDataSave.loadPlayerData(uuid);
    }

    public boolean deletePlayerData(UUID uuid) {
        return PlayerDataSave.deletePlayerData(uuid);
    }
}
