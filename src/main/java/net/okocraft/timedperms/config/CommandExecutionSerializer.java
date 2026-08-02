package net.okocraft.timedperms.config;

import com.github.siroshun09.configapi.api.Configuration;
import com.github.siroshun09.configapi.api.MappedConfiguration;
import com.github.siroshun09.configapi.api.serializer.ConfigurationSerializer;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.node.types.PermissionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandExecutionSerializer implements ConfigurationSerializer<CommandExecution> {
    
    public static CommandExecutionSerializer SERIALIZER = new CommandExecutionSerializer();
    
    private CommandExecutionSerializer() {
    }
    
    @Override
    public @NotNull Configuration serialize(@NotNull CommandExecution value) {
        Configuration config = MappedConfiguration.create();
        config.set("trigger", value.trigger());
        config.set("execution-offset", value.executionOffset());
        config.set("target-permission.key", value.targetPermission().getKey());
        value.targetPermission().getContexts().toMap().forEach(
                (context, values) -> config.set("target-permission.context." + context, new ArrayList<>(values)));
        config.set("player-permission", value.playerPermission());
        PlaceholderRequirement placeholderRequirement = value.placeholderRequirement();
        if (!placeholderRequirement.placeholder().isEmpty()) {
            config.set("placeholder-requirement.placeholder", placeholderRequirement.placeholder());
        }
        if (!placeholderRequirement.regex().isEmpty()) {
            config.set("placeholder-requirement.regex", placeholderRequirement.regex());
        }
        if (placeholderRequirement.moreThan() != Double.MAX_VALUE) {
            config.set("placeholder-requirement.more-than", placeholderRequirement.moreThan());
        }
        if (placeholderRequirement.lessThan() != Double.MIN_VALUE) {
            config.set("placeholder-requirement.less-than", placeholderRequirement.lessThan());
        }
        if (!placeholderRequirement.commandsOnDenied().isEmpty()) {
            config.set("placeholder-requirement.commands-on-denied", placeholderRequirement.commandsOnDenied());
        }
        config.set("command-source", value.isConsoleSource() ? "console" : "player");
        config.set("commands", value.commands());
        return config;
    }

    @Override
    public @Nullable CommandExecution deserializeConfiguration(@NotNull Configuration config) {
        TriggerType trigger = TriggerType.match(config.getString("trigger"), TriggerType.EXPIRE);
        int executionOffsetTemp = config.getInteger("execution-offset", 0);

        String targetPermissionKey = config.getString("target-permission.key");
        if (targetPermissionKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid configuration value: target-permission.key");
        }
        PermissionNode.Builder nodeBuilder = PermissionNode.builder(targetPermissionKey);
        Configuration contextsSection = config.getSection("target-permission.context");
        if (contextsSection != null) {
            for (String context : contextsSection.getKeyList()) {
                Object raw = contextsSection.get(context);
                if (raw != null) {
                    if (raw instanceof List<?>) {
                        ((List<?>) raw).forEach(value -> nodeBuilder.withContext(context, value.toString()));
                    } else {
                        nodeBuilder.withContext(context, raw.toString());
                    }
                }
            }
        }

        return new CommandExecution(
                trigger,
                trigger == TriggerType.EXPIRE ? executionOffsetTemp : Math.max(executionOffsetTemp, 0),
                nodeBuilder.build(),
                config.getString("player-permission"),
                config.getString("command-source").equalsIgnoreCase("console"),
                new PlaceholderRequirement(
                        config.getString("placeholder-requirement.placeholder"),
                        config.getString("placeholder-requirement.regex"),
                        config.getDouble("placeholder-requirement.more-than", Double.MAX_VALUE),
                        config.getDouble("placeholder-requirement.less-than", Double.MIN_VALUE),
                        config.getStringList("placeholder-requirement.commands-on-denied")
                ),
                new ArrayList<>(config.getStringList("commands"))
        );
    }
}
