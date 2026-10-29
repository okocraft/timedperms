package net.okocraft.timedperms.config;

import java.util.List;
import java.util.regex.Pattern;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.placeholderapi.PlaceholderHook;
import org.bukkit.OfflinePlayer;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.serialize.SerializationException;

@ConfigSerializable
public final class PlaceholderRequirement {

    private String placeholder = "";
    private String regex = "";
    private double moreThan = Double.MAX_VALUE;
    private double lessThan = Double.MIN_VALUE;
    private List<String> commandsOnDenied = List.of();

    @PostProcess
    private void prepare() throws SerializationException {
        Pattern.compile(regex);
        if (!Double.isFinite(moreThan) || !Double.isFinite(lessThan)
                || (moreThan != Double.MAX_VALUE && lessThan != Double.MIN_VALUE && moreThan > lessThan)) {
            throw new SerializationException("Invalid placeholder-requirement numeric bounds");
        }
        commandsOnDenied = List.copyOf(commandsOnDenied);
    }

    public List<String> commandsOnDenied() {
        return commandsOnDenied;
    }

    private static Main plugin;

    private Main getPlugin() {
        if (plugin == null) {
            plugin = Main.getPlugin(Main.class);
        }
        return plugin;
    }

    public boolean check(OfflinePlayer playerContext) {
        if (placeholder.isEmpty()) {
            return true;
        }

        PlaceholderHook hook = getPlugin().getPlaceholderHook();
        if (hook == null) {
            return false;
        }

        String replaced = hook.setPlaceholder(playerContext, placeholder);
        if (!regex.isEmpty() && !replaced.matches(regex)) {
            return false;
        }

        try {
            if (moreThan != Double.MAX_VALUE) {
                double value = Double.parseDouble(replaced);
                if (value < moreThan) {
                    return false;
                }
            }
            if (lessThan != Double.MIN_VALUE) {
                double value = Double.parseDouble(replaced);
                if (value > lessThan) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

}
