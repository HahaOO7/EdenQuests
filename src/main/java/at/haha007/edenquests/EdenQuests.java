package at.haha007.edenquests;

import at.haha007.edencommands.eden.CommandRegistry;
import at.haha007.edenconfig.core.InstanceCreatorMap;
import at.haha007.edenconfig.paper.PaperConfigurationManager;
import at.haha007.edenconfig.paper.yaml.EnumInstanceCreator;
import at.haha007.edenconfig.paper.yaml.NoArgsInstanceCreator;
import at.haha007.edenquests.config.SoundInstanceCreator;
import at.haha007.edenquests.messages.ConfigurableMessage;
import at.haha007.edenquests.messages.QuestMessages;
import at.haha007.edenquests.player.QuestPlayer;
import at.haha007.edenquests.player.QuestPlayerController;
import at.haha007.edenquests.quest.Quest;
import at.haha007.edenquests.quest.QuestHandler;
import at.haha007.edenquests.quest.QuestType;
import at.haha007.edenquests.quest.Reward;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

@Accessors(fluent = true)
public final class EdenQuests extends JavaPlugin {

    public static EdenQuests INSTANCE;

    @Getter
    private Economy economy;
    @Getter
    private PaperConfigurationManager configManager;
    @Getter
    private Map<String, Quest> questMap;
    @Getter
    private SessionFactory sessionFactory;
    @Getter
    private QuestPlayerController questPlayerController;
    @Getter
    private QuestCommand command;
    private QuestPapiExpansion papiExpansion;
    private long lastTick = System.currentTimeMillis();
    private File lastTickFile;


    @Override
    public void onEnable() {
        INSTANCE = this;
        lastTickFile = new File(getDataFolder(), "time.bin");
        initConfigManager();

        saveDefaultConfig();
        reloadConfig();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) throw new RuntimeException("Vault not found!");
        economy = rsp.getProvider();

        papiExpansion = new QuestPapiExpansion();
        papiExpansion.register();

        initDatabase();

        new QuestHandler(this);
        this.command = new QuestCommand(new CommandRegistry(this));

        loadTime();
        TimeZone timezone = TimeZone.getTimeZone(ZoneId.of(Objects.requireNonNull(getConfig().getString("timezone"))));

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                int oldDow = getDay(lastTick);
                int nextDow = getDay(now);
                lastTick = now;
                if (oldDow == nextDow) return;
                questPlayerController.resetAll();
            }

            int getDay(long time) {
                SimpleDateFormat sdf = new SimpleDateFormat("DD");
                sdf.setTimeZone(timezone);
                String formatted = sdf.format(new Date(time));
                return formatted.hashCode();
            }
        }, 1, 1200); // once a minute is enough
    }

    @SneakyThrows
    private void saveTime() {
        @Cleanup FileOutputStream fos = new FileOutputStream(lastTickFile);
        @Cleanup DataOutputStream dos = new DataOutputStream(fos);
        dos.writeLong(lastTick);
        dos.flush();
    }

    @SneakyThrows
    private void loadTime() {
        if (!lastTickFile.exists()) saveTime();
        @Cleanup FileInputStream fis = new FileInputStream(lastTickFile);
        @Cleanup DataInputStream dis = new DataInputStream(fis);
        lastTick = dis.readLong();
    }

    private void initConfigManager() {
        InstanceCreatorMap<ConfigurationSection> instanceCreatorMap = PaperConfigurationManager.GLOBAL_YAML_INSTANCE_CREATOR_MAP.clone();
        instanceCreatorMap.add(new SoundInstanceCreator());
        instanceCreatorMap.add(new NoArgsInstanceCreator<>(Reward.class, instanceCreatorMap));
        instanceCreatorMap.add(new NoArgsInstanceCreator<>(ConfigurableMessage.class, instanceCreatorMap));
        instanceCreatorMap.add(new NoArgsInstanceCreator<>(QuestMessages.class, instanceCreatorMap));
        instanceCreatorMap.add(new EnumInstanceCreator<>(Material.class));
        instanceCreatorMap.add(new EnumInstanceCreator<>(QuestType.class));
        configManager = PaperConfigurationManager.builder().plugin(this).yamlInstanceCreatorMap(instanceCreatorMap).build();
    }


    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration cfg = getConfig();
        questMap = new HashMap<>();
        ConfigurationSection questSection = Objects.requireNonNull(cfg.getConfigurationSection("quests"));
        questSection.getKeys(false).stream()
                .map(questSection::getConfigurationSection)
                .filter(Objects::nonNull)
                .forEach(q -> {
                    Quest quest = new Quest(q.getName());
                    configManager.createYamlConfigurator().config(q).inject(quest);
                    questMap.put(q.getName(), quest);
                });
    }

    private void initDatabase() {
        saveResource("hibernate.cfg.xml", false);
        sessionFactory = new Configuration()
                .configure(new File(this.getDataFolder(), "hibernate.cfg.xml"))
                .addAnnotatedClass(QuestPlayer.class)
                .buildSessionFactory();
        questPlayerController = new QuestPlayerController(sessionFactory);
    }

    @Override
    public void onDisable() {
        Optional.ofNullable(questPlayerController).ifPresent(QuestPlayerController::shutdown);
        Optional.ofNullable(sessionFactory).ifPresent(SessionFactory::close);
        Optional.ofNullable(papiExpansion).ifPresent(PlaceholderExpansion::unregister);
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        saveTime();
    }

    public void reload() {
        HandlerList.unregisterAll(this);
        command = new QuestCommand(new CommandRegistry(this));
        reloadConfig();
        new QuestHandler(this);
    }

}
