package at.haha007.edenquests.config;

import at.haha007.edenconfig.core.InstanceCreator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class SoundInstanceCreator implements InstanceCreator<ConfigurationSection, Sound> {

    public Class<Sound> getType() {
        return Sound.class;
    }

    public void save(ConfigurationSection config, Sound sound, String key) {
        config = config.createSection(key);
        if (sound == null)
            return;
        config.set("name", sound.name().asString());
        config.set("pitch", sound.pitch());
        config.set("source", sound.source().name());
        config.set("volume", sound.volume());
    }

    public Sound create(ConfigurationSection config, String key) {
        config = config.getConfigurationSection(key);
        if (config == null)
            return null;
        //noinspection PatternValidation
        Key name = Key.key(Objects.requireNonNull(config.getString("name")));
        Sound.Source source = Sound.Source.valueOf(config.getString("source", "master").toUpperCase());
        float volume = (float) config.getDouble("volume", 1);
        float pitch = (float) config.getDouble("pitch", 1);

        return Sound.sound(name, source, volume, pitch);
    }
}
