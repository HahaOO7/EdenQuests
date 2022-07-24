package at.haha007.edenquests.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public interface MessageMapper extends BiFunction<String, Player, Component> {
}
