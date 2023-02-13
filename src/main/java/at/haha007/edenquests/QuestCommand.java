package at.haha007.edenquests;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandException;
import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.argument.Argument;
import at.haha007.edencommands.argument.ParsedArgument;
import at.haha007.edencommands.argument.player.OfflinePlayerArgument;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edenconfig.core.ConfigInjected;
import at.haha007.edenquests.messages.ConfigurableMessage;
import at.haha007.edenquests.player.QuestPlayer;
import at.haha007.edenquests.player.QuestPlayerController;
import at.haha007.edenquests.quest.Quest;
import at.haha007.edenquests.quest.QuestType;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public class QuestCommand {
    private final EdenQuests plugin = EdenQuests.INSTANCE;
    private final QuestPlayerController playerProvider = plugin.questPlayerController();
    private final Random rand = new Random();
    @ConfigInjected
    @Getter
    private int maxSkips;
    @ConfigInjected
    @Getter
    private int maxDailyQuests;
    @ConfigInjected
    private ConfigurableMessage notEnoughItems;
    @ConfigInjected
    private ConfigurableMessage defaultMessage;
    @ConfigInjected
    private ConfigurableMessage skipMessage;
    @ConfigInjected
    private ConfigurableMessage noQuest;
    @ConfigInjected
    private ConfigurableMessage noQuestLeft;
    @ConfigInjected
    private ConfigurableMessage noSkipsLeft;


    public QuestCommand() {
        CommandRegistry registry = new CommandRegistry(EdenQuests.INSTANCE);
        plugin.configManager()
                .createYamlPipeline(new File(plugin.getDataFolder(), "config.yml"))
                .inject(this);

        LiteralCommandNode.LiteralCommandBuilder cmd = CommandRegistry.literal("quest");
        cmd.executor(context -> {
            if (!(context.sender() instanceof Player player)) return;
            defaultMessage.accept(player);
        });
        Argument<Quest> questArgument = new Argument<>(c -> plugin.questMap().keySet().stream().map(AsyncTabCompleteEvent.Completion::completion).toList(),true) {
            @Override
            public @NotNull ParsedArgument<Quest> parse(CommandContext context) throws CommandException {
                Quest quest = plugin.questMap().get(context.input()[context.pointer()]);
                if (quest == null) throw new CommandException(Component.text(""), context);
                return new ParsedArgument<>(quest, 1);
            }
        };

        //default commands
        cmd.then(CommandRegistry.literal("get").requires(Predicate.not(this::hasQuest)).executor(this::getQuest));
        cmd.then(CommandRegistry.literal("done").requires(this::hasCollectQuest).executor(this::questDone));
        cmd.then(CommandRegistry.literal("info").requires(this::hasQuest).executor(this::questInfo));
        cmd.then(CommandRegistry.literal("skip").requires(this::canSkip).executor(this::skipQuest));

        //management commands
        cmd.then(CommandRegistry.literal("reload").requires(CommandRegistry.permission("quest.command.reload")).executor(
                c -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.reload();
                    c.sender().sendMessage(Component.text("Quests reloaded!", NamedTextColor.GOLD));
                }))
        );
        cmd.then(CommandRegistry.literal("reset").requires(CommandRegistry.permission("quest.command.reset"))
                .then(CommandRegistry.literal("*").executor(c -> {
                    plugin.questPlayerController().resetAll();
                    c.sender().sendMessage(Component.text("[Quests] All players were reset!", NamedTextColor.GOLD));
                }))
                .then(CommandRegistry.argument("player", OfflinePlayerArgument.builder()
                                .playerNotFoundErrorProvider(c -> Component.text("player not found", NamedTextColor.GOLD))
                                .build())
                        .executor(c -> {
                    plugin.questPlayerController().reset(c.<OfflinePlayer>parameter("player").getUniqueId());
                    c.sender().sendMessage(Component.text("[Quests] Player was reset!", NamedTextColor.GOLD));
                })).executor(c -> c.sender().sendMessage(Component.text("/quest reset <player>", NamedTextColor.GOLD)))
        );
        cmd.then(CommandRegistry.literal("assign").requires(CommandRegistry.permission("quest.command.assign"))
                .then(CommandRegistry.argument("player", OfflinePlayerArgument.builder()
                                .playerNotFoundErrorProvider(c -> Component.text("player not found", NamedTextColor.GOLD))
                                .build())
                        .then(CommandRegistry.argument("quest", questArgument).executor(c -> {
                            OfflinePlayer op = c.parameter("player");
                            Quest quest = c.parameter("quest");
                            QuestPlayer qp = plugin.questPlayerController().get(op.getUniqueId());
                            qp.progress(0);
                            qp.setActiveQuest(quest);
                            c.sender().sendMessage(Component.text("Quest assigned!", NamedTextColor.GOLD));
                        }))
                )
        );

        cmd.requires(CommandRegistry.permission("quest.command.use"));
        registry.register(cmd.build());
    }

    private boolean canSkip(CommandSender sender) {
        if (!(sender instanceof Player p))
            return false;
        QuestPlayer player = playerProvider.get(p.getUniqueId());
        Quest quest = player.getActiveQuest();
        return quest != null && player.questsSkippedToday() < maxSkips;
    }

    private void skipQuest(CommandContext context) {
        if (!(context.sender() instanceof Player player))
            return;
        QuestPlayer qp = playerProvider.get(player.getUniqueId());
        Quest quest = qp.getActiveQuest();
        if (quest == null) {
            noQuest.accept(player);
            return;
        }

        if (qp.questsSkippedToday() >= maxSkips) {
            noSkipsLeft.accept(player);
            return;
        }

        qp.questsSkippedToday(qp.questsSkippedToday() + 1);
        qp.setActiveQuest(null);
        skipMessage.accept(player);
    }

    private void questInfo(CommandContext context) {
        if (!(context.sender() instanceof Player player))
            return;
        QuestPlayer qp = playerProvider.get(player.getUniqueId());
        Quest quest = qp.getActiveQuest();
        if (quest == null) {
            noQuest.accept(player);
            return;
        }
        quest.getMessages().infoMessage().accept(player);
    }

    private void questDone(CommandContext context) {
        if (!(context.sender() instanceof Player player))
            return;
        QuestPlayer qp = playerProvider.get(player.getUniqueId());
        Quest quest = qp.getActiveQuest();
        if (quest == null || quest.getType() != QuestType.COLLECT) {
            noQuest.accept(player);
            return;
        }
        Set<Material> materials = quest.getFilter().stream().map(String::toUpperCase).map(Material::valueOf).collect(Collectors.toSet());
        Inventory inventory = player.getInventory();
        AtomicInteger inventoryAmount = new AtomicInteger();
        inventory.forEach(i -> {
            if (i == null) return;
            if (!materials.contains(i.getType())) return;
            inventoryAmount.addAndGet(i.getAmount());
        });
        int amount = quest.getAmount();

        if (inventoryAmount.get() < amount) {
            notEnoughItems.accept(player);
            return;
        }

        for (ItemStack is : inventory) {
            if (is == null)
                continue;
            if (!materials.contains(is.getType()))
                continue;
            int delta = Math.min(amount, is.getAmount());
            is.setAmount(is.getAmount() - delta);
            amount -= delta;
            if (amount <= 0) break;
        }

        qp.questsDoneToday(qp.questsDoneToday() + 1);
        qp.setActiveQuest(null);
        quest.getReward().give(player);
        quest.getMessages().doneMessage().accept(player);
    }

    private void getQuest(CommandContext context) {
        if (!(context.sender() instanceof Player player))
            return;

        QuestPlayer qp = playerProvider.get(player.getUniqueId());

        if (maxDailyQuests <= qp.questsDoneToday()) {
            noQuestLeft.accept(player);
            return;
        }

        Map<String, Quest> questMap = plugin.questMap();
        List<Quest> quests = questMap.values().stream().toList();
        Quest quest = quests.get(rand.nextInt(quests.size()));
        qp.setActiveQuest(quest);
        qp.progress(0);
        quest.getMessages().getMessage().accept(player);
    }

    private boolean hasCollectQuest(CommandSender sender) {
        if (!(sender instanceof Player p))
            return false;
        QuestPlayer player = playerProvider.get(p.getUniqueId());
        Quest quest = player.getActiveQuest();
        return quest != null && quest.getType() == QuestType.COLLECT;
    }


    private boolean hasQuest(CommandSender sender) {
        return sender instanceof Player p && playerProvider.get(p.getUniqueId()).getActiveQuest() != null;
    }

}
