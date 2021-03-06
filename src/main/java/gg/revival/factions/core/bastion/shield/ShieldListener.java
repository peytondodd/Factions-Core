package gg.revival.factions.core.bastion.shield;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import gg.revival.factions.core.FC;
import gg.revival.factions.core.PlayerManager;
import gg.revival.factions.obj.FPlayer;
import gg.revival.factions.timers.TimerType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class ShieldListener implements Listener {

    @Getter private FC core;

    public ShieldListener(FC core) {
        this.core = core;
    }

    /**
     * Contains all players who currently have data being processed for their shields
     */
    private final Set<UUID> currentlyProcessing = Sets.newSetFromMap(Maps.<UUID, Boolean>newConcurrentMap());

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ShieldPlayer shieldPlayer = new ShieldPlayer(player.getUniqueId());
        core.getBastion().getShield().getShieldPlayers().add(shieldPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ShieldPlayer shieldPlayer = core.getBastion().getShield().getShieldPlayer(player.getUniqueId());
        core.getBastion().getShield().getShieldPlayers().remove(shieldPlayer);

        if(currentlyProcessing.contains(player.getUniqueId()))
            currentlyProcessing.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo(), from = event.getFrom();

        if(from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) return;

        if(currentlyProcessing.contains(player.getUniqueId())) return;
        final ShieldPlayer shieldPlayer = core.getBastion().getShield().getShieldPlayer(player.getUniqueId());
        final FPlayer facPlayer = PlayerManager.getPlayer(player.getUniqueId());

        if(
                !facPlayer.isBeingTimed(TimerType.TAG) &&
                !facPlayer.isBeingTimed(TimerType.PVPPROT) &&
                !facPlayer.isBeingTimed(TimerType.PROGRESSION)) {

            if(!currentlyProcessing.contains(player.getUniqueId()) && shieldPlayer.getLastShownBlocks() != null && !shieldPlayer.getLastShownBlocks().isEmpty())
            {
                currentlyProcessing.add(player.getUniqueId());

                new BukkitRunnable() {
                    public void run() {
                        for(BlockPos lastShown : shieldPlayer.getLastShownBlocks())
                            player.sendBlockChange(lastShown.getBukkitLocation(), Material.AIR, (byte)0);

                        shieldPlayer.setLastShownBlocks(null);
                        currentlyProcessing.remove(shieldPlayer.getUuid());
                    }
                }.runTaskAsynchronously(core);
            }

            return;
        }

        currentlyProcessing.add(player.getUniqueId());
        BlockPos currentPos =
                new BlockPos(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(), player.getLocation().getBlockZ(),
                player.getLocation().getWorld().getName());

        ShieldUpdateRequest request = new ShieldUpdateRequest(currentPos, shieldPlayer);
        final ShieldUpdateTask task = new ShieldUpdateTask(core, request);

        Bukkit.getScheduler().runTaskAsynchronously(core, task);
        task.addListener(() -> currentlyProcessing.remove(shieldPlayer.getUuid()), MoreExecutors.sameThreadExecutor());
    }

}
