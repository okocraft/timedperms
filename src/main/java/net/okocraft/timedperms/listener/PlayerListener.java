package net.okocraft.timedperms.listener;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.okocraft.timedperms.model.LocalPlayerFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    public void subscribeLuckPermsEvents() {
        LuckPermsProvider.get().getEventBus().subscribe(NodeMutateEvent.class, this::onNodeMutate);
        LuckPermsProvider.get().getEventBus().subscribe(NodeRemoveEvent.class, this::onNodeRemove);
        LuckPermsProvider.get().getEventBus().subscribe(NodeAddEvent.class, this::onNodeAdd);
        LuckPermsProvider.get().getEventBus().subscribe(NodeClearEvent.class, this::onNodeClear);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        LocalPlayerFactory.get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        LocalPlayerFactory.get(event.getPlayer().getUniqueId()).saveAndClose();
    }

    private void onNodeMutate(NodeMutateEvent event) {
        onNodeChange(event.getTarget(), event.getDataBefore(), event.getDataAfter());
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        onNodeChange(event.getTarget(), event.getDataBefore(), event.getDataAfter());
    }

    private void onNodeAdd(NodeAddEvent event) {
        onNodeChange(event.getTarget(), event.getDataBefore(), event.getDataAfter());
    }

    private void onNodeClear(NodeClearEvent event) {
        onNodeChange(event.getTarget(), event.getDataBefore(), event.getDataAfter());
    }

    private void onNodeChange(PermissionHolder permissionHolder, Set<Node> before, Set<Node> after) {
        if (!(permissionHolder instanceof User)) {
            return;
        }

        User user = (User) permissionHolder;
        LocalPlayerFactory.get(user.getUniqueId()).onPermissionRemoved(
                before.stream()
                        .filter(node -> node instanceof PermissionNode)
                        .map(node -> (PermissionNode) node)
                        .filter(Predicate.not(after::contains))
                        .collect(Collectors.toSet())
        );
    }
}