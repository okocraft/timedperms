package net.okocraft.timedperms.language;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import dev.siroshun.mcmsgdef.Placeholder;
import java.util.Map;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public final class Messages {

    private static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();

    private static final Placeholder<String> PLAYER = player -> Argument.string("player", player);
    private static final Placeholder<String> PERMISSION = permission -> Argument.string("permission", permission);
    private static final Placeholder<Integer> SECONDS = seconds -> Argument.numeric("seconds", seconds);
    private static final Placeholder<Integer> REMAINING_SECONDS = seconds -> Argument.numeric("remaining_seconds", seconds);
    private static final Placeholder<String> SUBCOMMAND = subcommand -> Argument.string("subcommand", subcommand);

    // Retain the existing keys so administrators can reuse their translations.
    public static final MessageKey NO_PERMISSION = DEFINER.define(
            "no-permission", "<red>You do not have permission.</red>");
    public static final MessageKey NOT_ENOUGH_ARGUMENT = DEFINER.define(
            "not-enough-argument", "<red>Not enough arguments.</red>");
    public static final MessageKey PLAYER_NOT_FOUND = DEFINER.define(
            "player-not-found", "<red>Player not found.</red>");
    public static final MessageKey CANNOT_PARSE_PERMISSION_CONTEXT = DEFINER.define(
            "cannot-parse-permission-context", "<red>Could not parse the permission context.</red>");
    public static final MessageKey INVALID_NUMBER = DEFINER.define(
            "invalid-number", "<red>Specify a positive integer for the time in seconds.</red>");

    public static final MessageKey.Arg3<String, String, Integer> SHOW = DEFINER.define(
            "command-timedperms-show",
            "<gray>Player <aqua><player></aqua>'s permission <aqua><permission></aqua> has <aqua><seconds></aqua> seconds remaining.</gray>"
    ).with(PLAYER, PERMISSION, SECONDS);
    public static final MessageKey.Arg4<String, String, Integer, Integer> REMOVE = DEFINER.define(
            "command-timedperms-remove",
            "<gray>Removed <aqua><seconds></aqua> seconds from player <aqua><player></aqua>'s permission <aqua><permission></aqua> (now <aqua><remaining_seconds></aqua> seconds).</gray>"
    ).with(PLAYER, PERMISSION, SECONDS, REMAINING_SECONDS);
    public static final MessageKey.Arg4<String, String, Integer, Integer> ADD = DEFINER.define(
            "command-timedperms-add",
            "<gray>Added <aqua><seconds></aqua> seconds to player <aqua><player></aqua>'s permission <aqua><permission></aqua> (now <aqua><remaining_seconds></aqua> seconds).</gray>"
    ).with(PLAYER, PERMISSION, SECONDS, REMAINING_SECONDS);
    public static final MessageKey.Arg3<String, String, Integer> SET = DEFINER.define(
            "command-timedperms-set",
            "<gray>Set the time for player <aqua><player></aqua>'s permission <aqua><permission></aqua> to <aqua><seconds></aqua> seconds.</gray>"
    ).with(PLAYER, PERMISSION, SECONDS);
    public static final MessageKey.Arg1<String> UNKNOWN_SUBCOMMAND = DEFINER.define(
            "unknown-subcommand",
            "<red>Unknown subcommand <aqua><subcommand></aqua>. Use <aqua>show</aqua>, <aqua>add</aqua>, <aqua>remove</aqua>, or <aqua>set</aqua>.</red>"
    ).with(SUBCOMMAND);

    @Contract(pure = true)
    public static @NotNull @UnmodifiableView Map<String, String> defaultMessages() {
        return DEFINER.getCollectedMessages();
    }

    private Messages() {
        throw new UnsupportedOperationException();
    }
}
