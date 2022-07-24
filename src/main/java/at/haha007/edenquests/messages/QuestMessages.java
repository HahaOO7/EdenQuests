package at.haha007.edenquests.messages;

import at.haha007.edenconfig.core.ConfigInjected;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class QuestMessages {
    @ConfigInjected(name = "get")
    private ConfigurableMessage getMessage;
    @ConfigInjected(name = "info")
    private ConfigurableMessage infoMessage;
    @ConfigInjected(name = "done")
    private ConfigurableMessage doneMessage;
    @ConfigInjected(name = "progress")
    private ConfigurableMessage progressMessage;
}
