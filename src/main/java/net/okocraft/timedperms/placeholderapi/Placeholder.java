package net.okocraft.timedperms.placeholderapi;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.node.types.PermissionNode;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.model.LocalPlayer;
import net.okocraft.timedperms.model.LocalPlayerFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Placeholder extends PlaceholderExpansion {

    private final Main plugin;

    public Placeholder(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPlugin() {
        return plugin.getName();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // identifier: %identifier_params% -> identifier
        // params: %identifier_params% -> params

        // format1: %timedperms_seconds_<player>_<permission>_<context=value>_...% (returns seconds left)
        // format2: %timedperms_seconds_<permission>_<context=value>_...% (executor required, returns seconds left)
        // format3: %timedperms_time_<player>_<permission>_<context=value>_...% (returns seconds left)
        // format4: %timedperms_time_<permission>_<context=value>_...% (executor required, returns seconds left)

        String[] args = params.split(params.contains(",") ? "," : "_", -1);

        String[] subArray = new String[args.length - 1];
        System.arraycopy(args, 1, subArray, 0, args.length - 1);
        int seconds = getSecondsFromArgs(player, subArray);
        if (args[0].equalsIgnoreCase("seconds")) {
            return String.valueOf(seconds);
        } else if (args[0].equalsIgnoreCase("time")) {
            return Duration.of(seconds, ChronoUnit.SECONDS).toString().substring(2).toLowerCase(Locale.ROOT);
        } else {
            return String.valueOf(seconds);
        }
    }

    public int getSecondsFromArgs(OfflinePlayer context, String[] args) {
        int firstContextIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("=")) {
                firstContextIndex = i;
                break;
            }
        }
        int permissionIndex = firstContextIndex != -1 ? firstContextIndex - 1 : args.length - 1;
        if (permissionIndex < 0) {
            return -1;
        }

        OfflinePlayer offlinePlayer;
        if (permissionIndex == 0) {
            offlinePlayer = context;
        } else {
            String[] nameSplit = new String[permissionIndex];
            System.arraycopy(args, 0, nameSplit, 0, permissionIndex);
            String name = String.join("_", nameSplit);
            try {
                offlinePlayer = plugin.getServer().getOfflinePlayer(UUID.fromString(name));
            } catch (IllegalArgumentException e) {
                offlinePlayer = plugin.getServer().getOfflinePlayer(name);
            }
        }

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return -1;
        }
        LocalPlayer localPlayer = LocalPlayerFactory.get(offlinePlayer.getUniqueId());

        PermissionNode.Builder builder = PermissionNode.builder(args[permissionIndex].toLowerCase(Locale.ROOT));

        for (int i = permissionIndex + 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("=")) {
                String[] argPart = arg.split("=", -1);
                if (argPart.length != 2) {
                    return -1;
                }
                builder.withContext(argPart[0], argPart[1]);
            } else {
                break;
            }
        }

        return localPlayer.getSeconds(builder.build());
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return onRequest(player, params);
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
}
