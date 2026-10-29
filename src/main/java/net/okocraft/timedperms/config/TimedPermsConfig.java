package net.okocraft.timedperms.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.luckperms.api.node.types.PermissionNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public record TimedPermsConfig(List<CommandExecution> commandExecutions) {

    public TimedPermsConfig {
        commandExecutions = List.copyOf(commandExecutions);
    }

    public static TimedPermsConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (var input = TimedPermsConfig.class.getResourceAsStream("/config.yml")) {
                if (input == null) {
                    throw new IOException("Bundled config.yml was not found");
                }
                Files.copy(input, path);
            }
        }

        ConfigurationNode root = YamlConfigurationLoader.builder().path(path).build().load();
        requireMapping(root);
        ConfigurationNode executionsNode = root.node("command-executions");
        requireList(executionsNode);
        List<CommandExecution> executions = new ArrayList<>();
        for (ConfigurationNode execution : executionsNode.childrenList()) {
            executions.add(loadExecution(execution));
        }
        return new TimedPermsConfig(executions);
    }

    private static CommandExecution loadExecution(ConfigurationNode node) throws SerializationException {
        if (!node.isMap()) {
            throw new SerializationException(node, CommandExecution.class, "Expected a command execution mapping");
        }

        String triggerName = node.node("trigger").get(String.class, "expire");
        TriggerType trigger;
        try {
            trigger = TriggerType.valueOf(triggerName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SerializationException(node.node("trigger"), TriggerType.class,
                    "Expected register, unregister, count, expire, add, remove, or set", e);
        }
        int offset = node.node("execution-offset").get(Integer.class, 0);

        ConfigurationNode permissionNode = node.node("target-permission");
        requireMapping(permissionNode);
        ConfigurationNode permissionKey = permissionNode.node("key");
        String permission = permissionKey.get(String.class, "");
        if (permission.isBlank()) {
            throw new SerializationException(permissionKey, String.class, "Expected a non-empty permission key");
        }
        PermissionNode.Builder builder;
        try {
            builder = PermissionNode.builder(permission);
        } catch (IllegalArgumentException e) {
            throw new SerializationException(permissionKey, PermissionNode.class, "Invalid permission key", e);
        }
        ConfigurationNode contexts = permissionNode.node("context");
        requireMapping(contexts);
        for (var entry : contexts.childrenMap().entrySet()) {
            ConfigurationNode context = entry.getValue();
            if (context.isNull()) {
                continue;
            }
            List<String> values = context.isList() ? strings(context) : List.of(context.get(String.class, ""));
            for (String value : values) {
                try {
                    builder.withContext(entry.getKey().toString(), value);
                } catch (IllegalArgumentException e) {
                    throw new SerializationException(context, String.class, "Invalid permission context", e);
                }
            }
        }

        String source = node.node("command-source").get(String.class, "player");
        if (!source.equalsIgnoreCase("console") && !source.equalsIgnoreCase("player")) {
            throw new SerializationException(node.node("command-source"), String.class, "Expected console or player");
        }

        ConfigurationNode requirement = node.node("placeholder-requirement");
        requireMapping(requirement);
        String regex = requirement.node("regex").get(String.class, "");
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new SerializationException(requirement.node("regex"), String.class, "Expected a valid regular expression", e);
        }
        double moreThan = requirement.node("more-than").get(Double.class, Double.MAX_VALUE);
        double lessThan = requirement.node("less-than").get(Double.class, Double.MIN_VALUE);
        if (!Double.isFinite(moreThan)) {
            throw new SerializationException(requirement.node("more-than"), Double.class, "Expected a finite number");
        }
        if (!Double.isFinite(lessThan)) {
            throw new SerializationException(requirement.node("less-than"), Double.class, "Expected a finite number");
        }
        if (moreThan != Double.MAX_VALUE && lessThan != Double.MIN_VALUE && moreThan > lessThan) {
            throw new SerializationException(requirement, PlaceholderRequirement.class,
                    "more-than must be less than or equal to less-than");
        }

        return new CommandExecution(
                trigger,
                trigger == TriggerType.EXPIRE ? offset : Math.max(offset, 0),
                builder.build(),
                node.node("player-permission").get(String.class, ""),
                source.equalsIgnoreCase("console"),
                new PlaceholderRequirement(
                        requirement.node("placeholder").get(String.class, ""),
                        regex, moreThan, lessThan,
                        strings(requirement.node("commands-on-denied"))
                ),
                strings(node.node("commands"))
        );
    }

    private static List<String> strings(ConfigurationNode node) throws SerializationException {
        requireList(node);
        return node.getList(String.class, List.of());
    }

    private static void requireMapping(ConfigurationNode node) throws SerializationException {
        if (!node.isNull() && !node.isMap()) {
            throw new SerializationException(node, TimedPermsConfig.class, "Expected a mapping");
        }
    }

    private static void requireList(ConfigurationNode node) throws SerializationException {
        if (!node.isNull() && !node.isList()) {
            throw new SerializationException(node, List.class, "Expected a list");
        }
    }
}
