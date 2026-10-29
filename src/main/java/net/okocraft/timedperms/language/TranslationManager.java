package net.okocraft.timedperms.language;

import dev.siroshun.mcmsgdef.directory.DirectorySource;
import dev.siroshun.mcmsgdef.directory.MessageProcessors;
import dev.siroshun.mcmsgdef.file.PropertiesFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TranslationManager {

    private static final Key LANGUAGE_KEY = Key.key("timedperms", "languages");

    private final Path languageDirectory;
    private Translator messageSource;

    public TranslationManager(Path pluginDirectory) {
        this.languageDirectory = pluginDirectory.resolve("languages");
    }

    public void load() throws IOException {
        Translator nextSource = DirectorySource.propertiesFiles(languageDirectory)
                .defaultLocale(Locale.ENGLISH, Locale.JAPANESE)
                .primaryLocale(Locale.ENGLISH)
                .messageProcessor(MessageProcessors.appendMissingMessagesToPropertiesFile(this::loadDefaultMessages))
                .loadAsMiniMessageTranslationStore(LANGUAGE_KEY);

        var globalTranslator = GlobalTranslator.translator();
        if (messageSource != null) {
            globalTranslator.removeSource(messageSource);
        }
        globalTranslator.addSource(nextSource);
        messageSource = nextSource;
    }

    public void unload() {
        if (messageSource != null) {
            GlobalTranslator.translator().removeSource(messageSource);
            messageSource = null;
        }
    }

    private @Nullable Map<String, String> loadDefaultMessages(@NotNull Locale locale) throws IOException {
        if (locale.equals(Locale.ENGLISH)) {
            return Messages.defaultMessages();
        }

        try (var input = TranslationManager.class.getResourceAsStream("/languages/" + locale + ".properties")) {
            if (input == null && locale.equals(Locale.JAPANESE)) {
                throw new IOException("Bundled Japanese messages were not found");
            }
            return input != null ? PropertiesFile.load(input) : null;
        }
    }
}
