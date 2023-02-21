package net.okocraft.timedperms.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.luckperms.api.node.types.PermissionNode;
import net.okocraft.timedperms.Main;
import net.okocraft.timedperms.luckpermsinternal.NodeJsonSerializer;
import org.bukkit.plugin.java.JavaPlugin;

final class LocalPlayerDataSerializer {
    
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private LocalPlayerDataSerializer() {
    }

    static Map<PermissionNode, Integer> loadFromFile(UUID player) {
        try (JsonReader reader = GSON.newJsonReader(new InputStreamReader(
                Files.newInputStream(preparePlayerDataFile(player)), StandardCharsets.UTF_8))) {

            JsonArray raw = GSON.fromJson(reader, JsonArray.class);
            if (raw != null) {
                return deserializeUserData(raw);
            } else {
                return new HashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void saveToFile(LocalPlayer player) {
        try (JsonWriter writer = GSON.newJsonWriter(new OutputStreamWriter(
                Files.newOutputStream(preparePlayerDataFile(player)), StandardCharsets.UTF_8))) {

            GSON.toJson(serializeUserData(player.getData()), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JsonArray serializeUserData(Map<PermissionNode, Integer> map) {
        JsonArray arr = new JsonArray();
        map.forEach((node, timeLeft) -> {
            JsonObject serialized = serializeNode(node);
            serialized.addProperty("timedperms-timeLeft", timeLeft);
            arr.add(serialized);
        });
        return arr;
    }

    static Map<PermissionNode, Integer> deserializeUserData(JsonArray data) {
        Map<PermissionNode, Integer> map = new HashMap<>();

        data.forEach(jsonObj -> {
            JsonObject copy = jsonObj.deepCopy().getAsJsonObject();
            int timeLeft = copy.remove("timedperms-timeLeft").getAsInt();
            map.put(deserializeNode(copy), timeLeft);
        });

        return map;
    }

    private static JsonObject serializeNode(PermissionNode node) {
        return NodeJsonSerializer.serializeNode(node, false).getAsJsonObject();
    }

    private static PermissionNode deserializeNode(JsonObject data) {
        return (PermissionNode) NodeJsonSerializer.deserializeNode(data);
    }

    private static Path preparePlayerDataFile(LocalPlayer player) {
        return preparePlayerDataFile(player.getUniqueId());
    }

    private static Path preparePlayerDataFile(UUID uid) {
        Path path = JavaPlugin.getPlugin(Main.class).getDataFolder().toPath()
                .resolve("data")
                .resolve(uid.toString() + ".json");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }
}