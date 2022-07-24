package at.haha007.edenquests.player;

import at.haha007.edenquests.EdenQuests;
import at.haha007.edenquests.quest.Quest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@Entity
@Table(name = "questPlayers")
@NoArgsConstructor
public class QuestPlayer {
    @Id
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private UUID uuid;

    @Column
    private String questKey;
    @Column
    private int progress;
    @Column
    int questsDoneToday;
    @Column
    int questsSkippedToday;


    public QuestPlayer(UUID uuid) {
        this.uuid = uuid;
    }


    @Nullable
    public Quest getActiveQuest() {
        if (questKey == null) return null;
        return EdenQuests.INSTANCE.questMap().get(questKey);
    }

    public void setActiveQuest(Quest quest) {
        questKey = quest == null ? null : quest.getKey();
    }
}
