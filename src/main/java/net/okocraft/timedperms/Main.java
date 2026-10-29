package net.okocraft.timedperms;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.okocraft.timedperms.command.TimedPermsCommand;
import net.okocraft.timedperms.config.TimedPermsConfig;
import net.okocraft.timedperms.language.TranslationManager;
import net.okocraft.timedperms.listener.PlayerListener;
import net.okocraft.timedperms.model.LocalPlayerFactory;
import net.okocraft.timedperms.placeholderapi.PlaceholderHook;
import net.okocraft.timedperms.scheduler.Scheduler;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private volatile TimedPermsConfig configuration;
    private final TranslationManager translationManager = new TranslationManager(getDataFolder().toPath());
    private final PlayerListener playerListener = new PlayerListener();
    private final Scheduler scheduler = Scheduler.create(this);
    private final TimedPermsCommand commandHandler = new TimedPermsCommand(this);
    private PlaceholderHook placeholderHook;
    private boolean messagesLoaded;

    @Override
    public void onLoad() {
        try {
            translationManager.load();
            messagesLoaded = true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load language files", e);
        }
    }

    @Override
    public void onEnable() {
        if (!messagesLoaded) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            configuration = TimedPermsConfig.load(getDataFolder().toPath().resolve("config.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load config.yml", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(playerListener, this);
        playerListener.subscribeLuckPermsEvents();

        PluginCommand command = Objects.requireNonNull(getCommand("timedperms"));
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        scheduler.scheduleRepeatingTask(() -> {
            try {
                for (Player p : getServer().getOnlinePlayers()) {
                    LocalPlayerFactory.get(p.getUniqueId()).countOne();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);

        try {
            placeholderHook = new PlaceholderHook(this);
            placeholderHook.register();
            getLogger().info("Using PlaceholderAPI.");
        } catch (NoClassDefFoundError ignored) {
        }
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }

        scheduler.shutdown();

        if (configuration != null) {
            for (Player p : getServer().getOnlinePlayers()) {
                LocalPlayerFactory.get(p.getUniqueId()).saveAndClose();
            }
        }

        playerListener.unsubscribeLuckPermsEvents();
        HandlerList.unregisterAll(playerListener);

        translationManager.unload();
    }

    public TimedPermsConfig getConfiguration() {
        return Objects.requireNonNull(configuration, "Configuration is not loaded");
    }

    public PlaceholderHook getPlaceholderHook() {
        return this.placeholderHook;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
}
