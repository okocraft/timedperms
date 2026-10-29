package net.okocraft.timedperms.config;

import java.util.List;
import java.util.Map;
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
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Matches;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.objectmapping.meta.Required;

@ConfigSerializable
public final class CommandExecution {

    private TriggerType trigger = TriggerType.EXPIRE;
    private int executionOffset;
    @Required
    private TargetPermission targetPermission;
    private String playerPermission = "";
    private CommandSource commandSource = CommandSource.PLAYER;
    private PlaceholderRequirement placeholderRequirement = new PlaceholderRequirement();
    private List<String> commands = List.of();
    private transient PermissionNode permissionNode;

    @PostProcess
    private void prepare() {
        executionOffset = trigger == TriggerType.EXPIRE ? executionOffset : Math.max(executionOffset, 0);
        PermissionNode.Builder builder = PermissionNode.builder(targetPermission.key());
        targetPermission.context().forEach((key, values) -> values.forEach(value -> builder.withContext(key, value)));
        permissionNode = builder.build();
        commands = List.copyOf(commands);
    }

    private enum CommandSource {
        CONSOLE, PLAYER
    }

    @ConfigSerializable
    private record TargetPermission(@Required @Matches("\\S+") String key, Map<String, List<String>> context) {
        private TargetPermission {
            context = context == null ? Map.of() : Map.copyOf(context);
        }
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
        if (!event.getPermission().equals(permissionNode)) {
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

            CommandSender sender = commandSource == CommandSource.CONSOLE ? Bukkit.getConsoleSender() : playerContext.getPlayer();
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
                .map(command -> command.replaceAll("%permission%", permissionNode.getPermission()))
                .collect(Collectors.toList());
    }
}
