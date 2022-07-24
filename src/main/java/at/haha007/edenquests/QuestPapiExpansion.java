package at.haha007.edenquests;

import at.haha007.edenquests.quest.Quest;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class QuestPapiExpansion extends PlaceholderExpansion {

    // quest_progress -> progress of current quest
    // quest_remaining -> reverse of quest_progress
    // quest_done-quests -> quests done today
    // quest_remaining-quests
    // quest_done-skips -> skips done today
    // quest_remaining-skips

    @Override
    public @NotNull String getIdentifier() {
        return "quest";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", EdenQuests.INSTANCE.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return EdenQuests.INSTANCE.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params) {
            case "progress" -> EdenQuests.INSTANCE.questPlayerController().get(player.getUniqueId()).progress() + "";
            case "remaining" -> getRemaining(player.getUniqueId());
            case "done-quests" ->
                    EdenQuests.INSTANCE.questPlayerController().get(player.getUniqueId()).questsDoneToday() + "";
            case "remaining-quests" -> EdenQuests.INSTANCE.command().maxDailyQuests() -
                    EdenQuests.INSTANCE.questPlayerController().get(player.getUniqueId()).questsDoneToday() + "";
            case "done-skips" ->
                    EdenQuests.INSTANCE.questPlayerController().get(player.getUniqueId()).questsSkippedToday() + "";
            case "remaining-skips" -> EdenQuests.INSTANCE.command().maxSkips() -
                    EdenQuests.INSTANCE.questPlayerController().get(player.getUniqueId()).questsSkippedToday() + "";
            default -> null;
        };
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return onRequest(player, params);
    }

    private String getRemaining(UUID uuid) {
        Quest quest = EdenQuests.INSTANCE.questPlayerController().get(uuid).getActiveQuest();
        if (quest == null)
            return null;
        return (quest.getAmount() - EdenQuests.INSTANCE.questPlayerController().get(uuid).progress()) + "";
    }
}
