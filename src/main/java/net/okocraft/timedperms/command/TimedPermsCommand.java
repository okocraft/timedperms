package net.okocraft.timedperms.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.node.types.PermissionNode;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.model.LocalPlayer;
import net.okocraft.timedperms.model.LocalPlayerFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimedPermsCommand implements CommandExecutor, TabExecutor {

    private final Main plugin;

    private final Set<String> playerNames;

    public TimedPermsCommand(Main plugin) {
        this.plugin = plugin;
        this.playerNames = Arrays.stream(this.plugin.getServer().getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("timedperms.use")) {
            sender.sendMessage(translatable("no-permission").color(NamedTextColor.RED));
            return true;
        }

        if (args.length <= 2) {
            sender.sendMessage(translatable("not-enough-argument").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer offlinePlayer;
        try {
            offlinePlayer = plugin.getServer().getOfflinePlayer(UUID.fromString(args[1]));
        } catch (IllegalArgumentException e) {
            offlinePlayer = plugin.getServer().getOfflinePlayer(args[1]);
        }

        if (!offlinePlayer.hasPlayedBefore() || offlinePlayer.getName() == null) {
            sender.sendMessage(translatable("player-not-found").color(NamedTextColor.RED));
            return true;
        }
        LocalPlayer player = LocalPlayerFactory.get(offlinePlayer.getUniqueId());

        PermissionNode.Builder nodeBuilder = PermissionNode.builder(args[2].toLowerCase(Locale.ROOT));

        int i;
        for (i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("=")) {
                String[] argPart = arg.split("=", -1);
                if (argPart.length != 2) {
                    sender.sendMessage(translatable("cannot-parse-permission-context").color(NamedTextColor.RED));
                    return true;
                }
                nodeBuilder.withContext(argPart[0], argPart[1]);
            } else {
                break;
            }
        }

        String subCommand = args[0];

        PermissionNode node = nodeBuilder.build();
        if (subCommand.equalsIgnoreCase("show")) {
            sender.sendMessage(translatable("command-timedperms-show").color(NamedTextColor.GREEN).arguments(
                    text(offlinePlayer.getName()).color(NamedTextColor.AQUA),
                    text(toString(node)).color(NamedTextColor.AQUA),
                    text(player.getSeconds(node)).color(NamedTextColor.AQUA)
            ));
            return true;
        }

        int secondDelta;
        if (args.length >= i + 1) {
            secondDelta = tryParse(args[i]).orElse(-1);
        } else if (subCommand.equalsIgnoreCase("remove")) {
            secondDelta = player.getSeconds(node);
        } else {
            sender.sendMessage(translatable("invalid-number").color(NamedTextColor.RED));
            return true;
        }
        if (secondDelta <= 0) {
            sender.sendMessage(translatable("invalid-number").color(NamedTextColor.RED));
            return true;
        }

        if (subCommand.equalsIgnoreCase("remove")) {
            int now = player.removeSeconds(node, secondDelta);
            sender.sendMessage(translatable("command-timedperms-remove").color(NamedTextColor.GREEN).arguments(
                    text(offlinePlayer.getName()).color(NamedTextColor.AQUA),
                    text(toString(node)).color(NamedTextColor.AQUA),
                    text(secondDelta).color(NamedTextColor.AQUA),
                    text(now).color(NamedTextColor.AQUA)
            ));
        } else if (subCommand.equalsIgnoreCase("add")) {
            int now = player.addSeconds(node, secondDelta);
            sender.sendMessage(translatable("command-timedperms-add").color(NamedTextColor.GREEN).arguments(
                    text(offlinePlayer.getName()).color(NamedTextColor.AQUA),
                    text(toString(node)).color(NamedTextColor.AQUA),
                    text(secondDelta).color(NamedTextColor.AQUA),
                    text(now).color(NamedTextColor.AQUA)
            ));
        } else if (subCommand.equalsIgnoreCase("set")) {
            int now = player.setSeconds(node, secondDelta);
            sender.sendMessage(translatable("command-timedperms-set").color(NamedTextColor.GREEN).arguments(
                    text(offlinePlayer.getName()).color(NamedTextColor.AQUA),
                    text(toString(node)).color(NamedTextColor.AQUA),
                    text(now).color(NamedTextColor.AQUA)
            ));
        } else {
            sender.sendMessage(translatable("unknown-subcommand")
                    .color(NamedTextColor.RED).arguments(text(subCommand)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("timedperms.use")) {
            return new ArrayList<>();
        }

        // timedperms show <player> <permission [context=value ...]>;
        // timedperms remove <player> <permission [context=value ...]> [time];
        // timedperms add <player> <permission [context=value ...]> <time>;
        // timedperms set <player> <permission [context=value ...]> <time>;

        List<String> subCommands = Arrays.asList("show", "remove", "add", "set");
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (!subCommands.contains(subCommand)) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return playerNames.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return Collections.singletonList("permission");
        }

        int i;
        for (i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("=")) {
                String[] argPart = arg.split("=", -1);
                if (argPart.length != 2) {
                    return new ArrayList<>();
                }
            } else {
                break;
            }
        }

        if (i + 1 == args.length) {
            if (subCommand.equalsIgnoreCase("show")) {
                return Collections.singletonList("context=value");
            } else {
                return Arrays.asList("context=value", "time");
            }
        } else {
            return new ArrayList<>();
        }
    }

    private static OptionalInt tryParse(String intStr) {
        try {
            return OptionalInt.of(Integer.parseInt(intStr));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private static String toString(PermissionNode node) {
        ContextSet contexts = node.getContexts();
        StringBuilder sb = new StringBuilder();
        contexts.forEach(c -> sb.append(c.getKey()).append("=").append(c.getValue()).append(","));
        return node.getPermission() + "(" + sb.substring(0, sb.length() - 1) + ")";
    }
}
