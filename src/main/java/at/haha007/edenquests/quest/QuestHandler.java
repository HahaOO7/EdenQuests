package at.haha007.edenquests.quest;

import at.haha007.edenquests.EdenQuests;
import at.haha007.edenquests.player.QuestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestHandler implements Listener {
    private final EdenQuests plugin = EdenQuests.INSTANCE;

    public QuestHandler(EdenQuests plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            QuestPlayer qp = plugin.questPlayerController().get(player.getUniqueId());
            Quest quest = qp.getActiveQuest();
            if (quest == null)
                return;
            if (quest.getType() != QuestType.MINE)
                return;
            boolean allowSilk = quest.getFlags() != null && quest.getFlags().contains("allow_silk");
            ItemMeta itemMeta = player.getInventory().getItemInMainHand().getItemMeta();
            boolean hasSilk = itemMeta != null && itemMeta.hasEnchant(Enchantment.SILK_TOUCH);
            if (!allowSilk && hasSilk)
                return;
            if (!quest.getFilter().contains(material.name().toLowerCase()))
                return;
            progressQuest(quest, qp, player);
        });
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onMobKill(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player player)) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Damageable damageable))
            return;
        if (event.getDamage() < damageable.getHealth())
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            QuestPlayer qp = plugin.questPlayerController().get(player.getUniqueId());
            Quest quest = qp.getActiveQuest();
            if (quest == null)
                return;
            if (quest.getType() != QuestType.KILL)
                return;
            if (!quest.getFilter().contains(entity.getType().name().toLowerCase()))
                return;
            boolean allowCustomSpawn = quest.getFlags() != null && quest.getFlags().contains("allow_custom_spawn");
            if (!allowCustomSpawn && event.getEntity().getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL)
                return;
            progressQuest(quest, qp, player);
        });
    }

    private synchronized void progressQuest(Quest quest, QuestPlayer qp, Player player) {
        qp.progress(qp.progress() + 1);
        quest.getMessages().progressMessage().accept(player);
        if (qp.progress() < quest.getAmount())
            return;
        qp.progress(0);
        qp.questsDoneToday(qp.questsDoneToday() + 1);
        qp.setActiveQuest(null);
        quest.getReward().give(player);
        quest.getMessages().doneMessage().accept(player);
    }
}
