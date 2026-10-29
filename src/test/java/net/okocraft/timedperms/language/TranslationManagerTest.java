package net.okocraft.timedperms.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.siroshun.mcmsgdef.file.PropertiesFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TranslationManagerTest {

    @TempDir
    Path dataDirectory;

    private TranslationManager manager;

    @BeforeEach
    void setUp() {
        manager = new TranslationManager(dataDirectory);
    }

    @AfterEach
    void tearDown() {
        manager.unload();
    }

    @Test
    void testLoadCreatesCompleteBaselineLocalesAndFallsBackToEnglish() throws IOException {
        manager.load();

        Map<String, String> english = loadProperties("en.properties");
        Map<String, String> japanese = loadProperties("ja.properties");
        assertEquals(Messages.defaultMessages(), english);
        assertEquals(english.keySet(), japanese.keySet());
        assertEquals("You do not have permission.", render(Messages.NO_PERMISSION, Locale.US));
        assertEquals("権限がありません。", render(Messages.NO_PERMISSION, Locale.JAPAN));
        assertEquals("You do not have permission.", render(Messages.NO_PERMISSION, Locale.GERMAN));
    }

    @Test
    void testLoadPreservesCustomValuesAndAppendsMissingDefaults() throws IOException {
        Path languages = Files.createDirectories(dataDirectory.resolve("languages"));
        Files.writeString(languages.resolve("en.properties"), "no-permission=<red>Custom denial.</red>\n");
        Files.writeString(languages.resolve("ja.properties"), "no-permission=<red>独自のメッセージ。</red>\n");

        manager.load();

        assertEquals("<red>Custom denial.</red>", loadProperties("en.properties").get("no-permission"));
        assertEquals("<red>独自のメッセージ。</red>", loadProperties("ja.properties").get("no-permission"));
        assertEquals(Messages.defaultMessages().keySet(), loadProperties("en.properties").keySet());
        assertEquals(Messages.defaultMessages().keySet(), loadProperties("ja.properties").keySet());
        assertEquals("Custom denial.", render(Messages.NO_PERMISSION, Locale.ENGLISH));
        assertEquals("独自のメッセージ。", render(Messages.NO_PERMISSION, Locale.JAPANESE));
    }

    @Test
    void testLegacyYamlFilesAreLeftUntouched() throws IOException {
        Path languages = Files.createDirectories(dataDirectory.resolve("languages"));
        String legacy = "command-timedperms-show: \"Custom {0} {1} {2}\"\n";
        Files.writeString(languages.resolve("en.yml"), legacy);
        Files.writeString(languages.resolve("ja_JP.yml"), legacy);

        manager.load();

        assertEquals(legacy, Files.readString(languages.resolve("en.yml")));
        assertEquals(legacy, Files.readString(languages.resolve("ja_JP.yml")));
        assertEquals(Messages.defaultMessages(), loadProperties("en.properties"));
    }

    @Test
    void testNamedArgumentsRenderInBothLanguagesWithoutParsingUserValues() throws IOException {
        manager.load();

        var show = Messages.SHOW.apply("Alex", "example.use", 30);
        assertEquals("Player Alex's permission example.use has 30 seconds remaining.", render(show, Locale.ENGLISH));
        assertEquals("プレイヤー Alex の権限 example.use は残り 30秒です。", render(show, Locale.JAPANESE));
        var add = Messages.ADD.apply("Alex", "example.use", 10, 40);
        assertEquals("Added 10 seconds to player Alex's permission example.use (now 40 seconds).", render(add, Locale.ENGLISH));
        assertEquals("プレイヤー Alex の権限 example.use の期限を 10秒増やしました（現在 40秒）。", render(add, Locale.JAPANESE));
        assertEquals("Unknown subcommand <red>custom</red>. Use show, add, remove, or set.",
                render(Messages.UNKNOWN_SUBCOMMAND.apply("<red>custom</red>"), Locale.ENGLISH));
    }

    @Test
    void testReloadReplacesSourceAndUnloadRemovesIt() throws IOException {
        manager.load();
        Files.writeString(dataDirectory.resolve("languages/en.properties"), "no-permission=Updated\n");

        manager.load();

        assertEquals("Updated", render(Messages.NO_PERMISSION, Locale.ENGLISH));
        assertEquals(1, countSources());
        manager.unload();
        manager.unload();
        assertEquals(0, countSources());
        assertEquals("no-permission", render(Messages.NO_PERMISSION, Locale.ENGLISH));
    }

    @Test
    void testFailedReloadKeepsPreviousTranslations() throws IOException {
        manager.load();
        Path languages = dataDirectory.resolve("languages");
        Files.move(languages, dataDirectory.resolve("previous-languages"));
        Files.writeString(languages, "This file blocks the language directory.");

        assertThrows(IOException.class, manager::load);

        assertEquals(1, countSources());
        assertEquals("You do not have permission.", render(Messages.NO_PERMISSION, Locale.ENGLISH));
        assertEquals("権限がありません。", render(Messages.NO_PERMISSION, Locale.JAPANESE));
    }

    private Map<String, String> loadProperties(String filename) throws IOException {
        try (var input = Files.newInputStream(dataDirectory.resolve("languages").resolve(filename))) {
            return PropertiesFile.load(input);
        }
    }

    private static String render(ComponentLike message, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(message.asComponent(), locale));
    }

    private static int countSources() {
        int count = 0;
        for (var source : GlobalTranslator.translator().sources()) {
            if (source.name().equals(Key.key("timedperms", "languages"))) {
                count++;
            }
        }
        return count;
    }
}
