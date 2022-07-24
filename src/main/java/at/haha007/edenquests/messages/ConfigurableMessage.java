package at.haha007.edenquests.messages;

import at.haha007.edenconfig.core.ConfigInjected;
import lombok.ToString;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;

@ToString
public class ConfigurableMessage implements Consumer<Player> {

    @ConfigInjected
    private String chat;
    @ConfigInjected
    private String title;
    @ConfigInjected
    private String subtitle;
    @ConfigInjected
    private String actionbar;
    @ConfigInjected
    private Sound sound;

    public void accept(Player player) {
        MessageMapper messageMapper = new PapiMessageMapper();
        Optional.ofNullable(chat).ifPresent(c -> player.sendMessage(messageMapper.apply(c, player)));
        Optional.ofNullable(actionbar).ifPresent(c -> player.sendActionBar(messageMapper.apply(c, player)));
        Optional.ofNullable(sound).ifPresent(player::playSound);
        if (Optional.ofNullable(title).isPresent() || Optional.ofNullable(subtitle).isPresent()) {
            player.showTitle(Title.title(
                    Optional.ofNullable(title).map(c -> messageMapper.apply(c, player)).orElse(Component.empty()),
                    Optional.ofNullable(subtitle).map(c -> messageMapper.apply(c, player)).orElse(Component.empty())
            ));
        }
    }
}
