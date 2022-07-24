package at.haha007.edenquests.quest;

import at.haha007.edenconfig.core.ConfigInjected;
import at.haha007.edenquests.EdenQuests;
import at.haha007.edenquests.messages.ConfigurableMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public class Reward {
    @ConfigInjected
    private Double money;
    @ConfigInjected
    private ConfigurableMessage broadcast;
    @ConfigInjected
    private String command;


    public void give(Player player) {
        if (money > 0)
            EdenQuests.INSTANCE.economy().depositPlayer(player, money);

        Optional.ofNullable(broadcast).ifPresent(msg -> Bukkit.getOnlinePlayers().forEach(msg));
        Optional.ofNullable(command).ifPresent(cmd -> Bukkit.getScheduler().runTask(EdenQuests.INSTANCE,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()))));
    }

}
