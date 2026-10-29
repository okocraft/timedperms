package net.okocraft.timedperms.config;

import java.util.List;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.placeholderapi.PlaceholderHook;
import org.bukkit.OfflinePlayer;

public record PlaceholderRequirement(
    String placeholder,
    String regex,
    Double moreThan,
    Double lessThan,
    List<String> commandsOnDenied
) {

    public PlaceholderRequirement {
        commandsOnDenied = List.copyOf(commandsOnDenied);
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
