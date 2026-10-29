package net.okocraft.timedperms.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.luckperms.api.node.types.PermissionNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

class ObjectMappingTest {
    @TempDir
    Path directory;
    private MockedStatic<PermissionNode> factory;
    private PermissionNode.Builder builder;
    private List<Map.Entry<String, String>> contexts;

    @BeforeEach
    void setUp() {
        contexts = new ArrayList<>();
        builder = mock(PermissionNode.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(mock(PermissionNode.class));
        when(builder.withContext(anyString(), anyString())).thenAnswer(call -> {
            contexts.add(Map.entry(call.getArgument(0), call.getArgument(1)));
            return builder;
        });
        factory = mockStatic(PermissionNode.class);
        factory.when(() -> PermissionNode.builder(anyString())).thenReturn(builder);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void testBundledConfigurationAndRecordsLoadWithoutRewriting() throws IOException {
        Path path = directory.resolve("nested/config.yml");
        var config = TimedPermsConfig.load(path);
        assertEquals(1, config.commandExecutions().size());
        var node = serialize(config).node("command-executions", 0);
        assertEquals("EXPIRE", node.node("trigger").getString());
        assertEquals(-5, node.node("execution-offset").getInt());
        assertEquals("CONSOLE", node.node("command-source").getString());
        assertEquals("test.use", node.node("target-permission", "key").getString());
        assertEquals(List.of("tell %player_name% test(%permission%)"), node.node("commands").getList(String.class));
        assertTrue(contexts.contains(Map.entry("othercontext", "true")));
        assertTrue(contexts.contains(Map.entry("world", "test1")));
        assertTrue(contexts.contains(Map.entry("world", "test2")));
        try (var resource = getClass().getResourceAsStream("/config.yml")) {
            assertArrayEquals(resource.readAllBytes(), Files.readAllBytes(path));
        }
    }

    @Test
    void testDefaultsAndOptionalContext() throws IOException {
        var config = TimedPermsConfig.load(write("command-executions:\n  - target-permission: {key: example.use}\n"));
        var node = serialize(config).node("command-executions", 0);
        assertEquals("EXPIRE", node.node("trigger").getString());
        assertEquals(0, node.node("execution-offset").getInt());
        assertEquals("PLAYER", node.node("command-source").getString());
        assertEquals("", node.node("player-permission").getString());
        assertEquals(List.of(), node.node("commands").getList(String.class));
        assertEquals(Double.MAX_VALUE, node.node("placeholder-requirement", "more-than").getDouble());
        assertEquals(Double.MIN_VALUE, node.node("placeholder-requirement", "less-than").getDouble());
        assertThrows(UnsupportedOperationException.class, () -> config.commandExecutions().clear());
        assertTrue(contexts.isEmpty());
        assertTrue(TimedPermsConfig.load(write("")).commandExecutions().isEmpty());
    }

    @Test
    void testScalarAndListContextsAndRoundTripSerialization() throws IOException {
        String yaml = """
                # Custom configuration
                extra: preserve
                command-executions:
                  - trigger: aDd
                    execution-offset: -5
                    target-permission:
                      key: example.use
                      context:
                        world: [one, two]
                        flag: true
                        number: 42
                    command-source: cOnSoLe
                    placeholder-requirement:
                      placeholder: '%value%'
                      regex: '[0-9]+'
                      more-than: 10
                      less-than: 20
                      commands-on-denied: [denied]
                    commands: [first, second]
                """;
        Path path = write(yaml);
        var config = TimedPermsConfig.load(path);
        assertEquals(yaml, Files.readString(path));
        assertTrue(contexts.containsAll(List.of(Map.entry("world", "one"), Map.entry("world", "two"),
                Map.entry("flag", "true"), Map.entry("number", "42"))));
        var node = serialize(config);
        var execution = node.node("command-executions", 0);
        assertEquals("ADD", execution.node("trigger").getString());
        assertEquals(0, execution.node("execution-offset").getInt());
        assertEquals("CONSOLE", execution.node("command-source").getString());
        assertEquals(List.of("denied"), execution.node("placeholder-requirement", "commands-on-denied").getList(String.class));
        assertTrue(execution.node("permission-node").virtual());
        var roundTrip = node.get(TimedPermsConfig.class);
        assertEquals(node.raw(), serialize(roundTrip).raw());
    }

    @ParameterizedTest
    @MethodSource("invalid")
    void testInvalidConfigurationIsRejected(String yaml) throws IOException {
        Path path = write(yaml);
        assertThrows(IOException.class, () -> TimedPermsConfig.load(path));
        assertEquals(yaml, Files.readString(path));
    }

    static Stream<String> invalid() {
        String prefix = "command-executions:\n  - target-permission: {key: example.use}\n    ";
        return Stream.of("command-executions: [", "command-executions:\n  - trigger: expire",
                "command-executions:\n  - target-permission: {key: '  '}",
                prefix + "trigger: invalid", prefix + "command-source: invalid",
                prefix + "execution-offset: invalid", prefix + "execution-offset: 1.5",
                prefix + "placeholder-requirement: {regex: '['}",
                prefix + "placeholder-requirement: {more-than: .nan}",
                prefix + "placeholder-requirement: {more-than: 20, less-than: 10}");
    }

    @Test
    void testJarContainsNoConfigurationRuntime() throws IOException {
        try (var jar = new JarFile(System.getProperty("verification.pluginJar"))) {
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("org/spongepowered/")
                    || entry.getName().contains("configapi") || entry.getName().startsWith("org/yaml/")));
        }
    }

    private ConfigurationNode serialize(TimedPermsConfig config) throws IOException {
        return BasicConfigurationNode.root().set(TimedPermsConfig.class, config);
    }

    private Path write(String yaml) throws IOException {
        return Files.writeString(directory.resolve("config.yml"), yaml);
    }
}
