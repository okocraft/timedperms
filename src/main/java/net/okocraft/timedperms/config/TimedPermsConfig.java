package net.okocraft.timedperms.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@ConfigSerializable
public final class TimedPermsConfig {

    private List<CommandExecution> commandExecutions = List.of();

    public List<CommandExecution> commandExecutions() {
        return commandExecutions;
    }

    @PostProcess
    private void prepare() {
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

        return YamlConfigurationLoader.builder().path(path).build().load()
                .get(TimedPermsConfig.class, new TimedPermsConfig());
    }
}
