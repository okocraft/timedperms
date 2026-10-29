package net.okocraft.timedperms.config;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.luckperms.api.node.types.PermissionNode;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.event.TimedPermissionCountEvent;
import net.okocraft.timedperms.event.TimedPermissionEvent;
import net.okocraft.timedperms.model.LocalPlayerFactory;
import net.okocraft.timedperms.placeholderapi.PlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public record CommandExecution(
    TriggerType trigger,
    int executionOffset,
    PermissionNode targetPermission,
    String playerPermission,
    boolean isConsoleSource,
    PlaceholderRequirement placeholderRequirement,
    List<String> commands
) {

    public CommandExecution {
        commands = List.copyOf(commands);
    }

    private static Main plugin;

    public static List<CommandExecution> getExecutions() {
        return getPlugin().getConfiguration().commandExecutions();
    }

    private static Main getPlugin() {
        if (plugin == null) {
            plugin = Main.getPlugin(Main.class);
        }
        return plugin;
    }

    public void handleEvent(TimedPermissionEvent event, OfflinePlayer playerContext) {
        if (!event.getPermission().equals(targetPermission)) {
            return;
        }

        if (executionOffset < 0) {
            if (trigger == TriggerType.EXPIRE) {
                if (event instanceof TimedPermissionCountEvent
                        && ((TimedPermissionCountEvent) event).getNewSeconds() + executionOffset == 0) {
                    scheduleCommands(playerContext, 0);
                }
                return;
            }
        } else if (executionOffset > 0) {
            if (trigger != TriggerType.COUNT) {
                if (trigger.getCorrespondingEventType() == event.getClass()) {
                    scheduleCommands(playerContext, executionOffset);
                }
                return;
            }
        }

        // offset == 0 || trigger == count || (seconds + offset == 0 && trigger == expire)
        if (trigger.getCorrespondingEventType() == event.getClass()) {
            scheduleCommands(playerContext, executionOffset);
        }
    }

    public void scheduleCommands(OfflinePlayer playerContext, int delay) {
        if (delay == 0) {
            executeCommands(playerContext);
        } else {
            plugin.getScheduler().scheduleDelayedTask(() -> executeCommands(playerContext), delay, TimeUnit.SECONDS);
        }
    }

    public void executeCommands(OfflinePlayer playerContext) {
        plugin.getScheduler().runOnGlobalScheduler(() -> {
            if (!playerPermission.isEmpty()
                    && !LocalPlayerFactory.get(playerContext.getUniqueId()).hasPermission(playerPermission)) {
                return;
            }

            CommandSender sender = isConsoleSource ? Bukkit.getConsoleSender() : playerContext.getPlayer();
            if (sender == null) {
                return;
            }

            if (!placeholderRequirement.check(playerContext)) {
                replaceCommands(playerContext, placeholderRequirement.commandsOnDenied())
                        .forEach(command -> Bukkit.dispatchCommand(sender, command));
            } else {
                replaceCommands(playerContext, commands)
                        .forEach(command -> Bukkit.dispatchCommand(sender, command));
            }
        });
    }

    private List<String> replaceCommands(OfflinePlayer playerContext, List<String> commands) {
        PlaceholderHook placeholder = getPlugin().getPlaceholderHook();
        return commands.stream()
                .map(command -> placeholder == null ? command : placeholder.setPlaceholder(playerContext, command))
                .map(command -> command.replaceAll("%permission%", targetPermission().getPermission()))
                .collect(Collectors.toList());
    }
}
