package at.haha007.edenquests.player;

import at.haha007.edenquests.EdenQuests;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestPlayerController implements Listener {

    private final EdenQuests plugin;
    private final Map<UUID, QuestPlayer> loaded = new ConcurrentHashMap<>();
    private final SessionFactory sessionFactory;


    @SneakyThrows
    public QuestPlayerController(SessionFactory sessionFactory) {
        this.plugin = EdenQuests.INSTANCE;
        this.sessionFactory = sessionFactory;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        get(event.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> unload(event.getPlayer().getUniqueId()));
    }

    public void shutdown() {
        loaded.keySet().iterator().forEachRemaining(this::unload);
    }

    @Blocking
    private void unload(UUID uuid) {
        QuestPlayer qp = loaded.get(uuid);
        if (qp == null)
            return;
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.merge(qp);
        session.getTransaction().commit();
        session.close();
        loaded.remove(uuid);
    }

    @Blocking
    @NotNull
    public QuestPlayer get(UUID uuid) {
        QuestPlayer qp = loaded.get(uuid);
        if (qp == null)
            qp = load(uuid);
        return qp;
    }

    public void reset(UUID uuid) {
        QuestPlayer qp = get(uuid);
        qp.progress(0);
        qp.setActiveQuest(null);
        qp.questsSkippedToday(0);
        qp.questsDoneToday(0);
    }

    public void resetAll() {
        for (QuestPlayer qp : loaded.values()) {
            qp.progress(0);
            qp.setActiveQuest(null);
            qp.questsSkippedToday(0);
            qp.questsDoneToday(0);
        }
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.createMutationQuery("DELETE FROM QuestPlayer");
        session.getTransaction().commit();
        session.close();
    }

    @NotNull
    private QuestPlayer load(UUID uuid) {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        QuestPlayer qp = session.get(QuestPlayer.class, uuid);
        session.getTransaction().commit();
        session.close();
        if (qp == null)
            qp = new QuestPlayer(uuid);
        loaded.put(qp.uuid(), qp);
        return qp;
    }
}
