package at.haha007.edenquests.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class PapiMessageMapper implements MessageMapper {
    LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public Component apply(String s, Player player) {
        return serializer.deserialize(PlaceholderAPI.setPlaceholders(player, s));
    }

}
