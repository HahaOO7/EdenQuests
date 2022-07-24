package at.haha007.edenquests.quest;

import at.haha007.edenconfig.core.ConfigInjected;
import at.haha007.edenquests.messages.QuestMessages;
import lombok.Getter;

import java.util.List;

@Getter
public final class Quest {
    private final String key;
    @ConfigInjected
    private int amount;
    @ConfigInjected
    private List<String> flags;
    @ConfigInjected
    private List<String> filter;
    @ConfigInjected
    private QuestMessages messages;
    @ConfigInjected
    private Reward reward;
    @ConfigInjected
    private QuestType type;

    public Quest(String name) {
        key = name;
    }
}
