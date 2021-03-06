package gg.revival.factions.core.db;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import gg.revival.driver.MongoAPI;
import gg.revival.factions.core.FC;
import gg.revival.factions.core.PlayerManager;
import gg.revival.factions.core.db.listener.DatabaseListener;
import gg.revival.factions.core.tools.Permissions;
import gg.revival.factions.obj.FPlayer;
import gg.revival.factions.timers.Timer;
import gg.revival.factions.timers.TimerManager;
import gg.revival.factions.timers.TimerType;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DBManager {

    @Getter private FC core;

    public DBManager(FC core) {
        this.core = core;

        onEnable();
    }

    @Getter @Setter MongoCollection<Document> bastion;
    @Getter @Setter MongoCollection<Document> deathbans;
    @Getter @Setter MongoCollection<Document> lives;
    @Getter @Setter MongoCollection<Document> stats;
    @Getter @Setter MongoCollection<Document> combatLoggers;

    /**
     * Saves a given FPlayer objects timer data to DB
     * @param player
     */
    public void saveTimerData(final FPlayer player, boolean unsafe) {
        final ImmutableList<Timer> timers = ImmutableList.copyOf(player.getTimers());

        if(unsafe) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            Runnable saveTask = () -> {
                if(bastion == null)
                    bastion = MongoAPI.getCollection(core.getConfiguration().databaseName, "bastion");

                MongoCollection<Document> collection = bastion;
                FindIterable<Document> query;

                try {
                    query = MongoAPI.getQueryByFilter(collection, "uuid", player.getUuid().toString());
                } catch (LinkageError err) {
                    saveTimerData(player, unsafe);
                    return;
                }

                int tagDuration = 0, protectionDuration = 0, progressionDuration = 0;

                for(Timer timer : timers) {
                    if(timer.getType().equals(TimerType.TAG))
                        tagDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);

                    if(timer.getType().equals(TimerType.PVPPROT))
                        protectionDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);

                    if(timer.getType().equals(TimerType.PROGRESSION))
                        progressionDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);
                }

                Document foundDocument = query.first();
                Document newDocument = new Document("uuid", player.getUuid().toString())
                        .append("tag", tagDuration).append("protection", protectionDuration).append("progression", progressionDuration);

                if(foundDocument != null)
                    collection.replaceOne(foundDocument, newDocument);
                else
                    collection.insertOne(newDocument);
            };

            executorService.execute(saveTask);
        }

        else {
            new BukkitRunnable() {
                public void run() {
                    if(bastion == null)
                        bastion = MongoAPI.getCollection(core.getConfiguration().databaseName, "bastion");

                    MongoCollection<Document> collection = bastion;
                    FindIterable<Document> query;

                    try {
                        query = MongoAPI.getQueryByFilter(collection, "uuid", player.getUuid().toString());
                    } catch (LinkageError err) {
                        saveTimerData(player, unsafe);
                        return;
                    }

                    int tagDuration = 0, protectionDuration = 0, progressionDuration = 0;

                    for(Timer timer : timers) {
                        if(timer.getType().equals(TimerType.TAG))
                            tagDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);

                        if(timer.getType().equals(TimerType.PVPPROT))
                            protectionDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);

                        if(timer.getType().equals(TimerType.PROGRESSION))
                            progressionDuration = (int)((timer.getExpire() - System.currentTimeMillis()) / 1000L);
                    }

                    Document foundDocument = query.first();
                    Document newDocument = new Document("uuid", player.getUuid().toString())
                            .append("tag", tagDuration).append("protection", protectionDuration).append("progression", progressionDuration);

                    if(foundDocument != null)
                        collection.replaceOne(foundDocument, newDocument);
                    else
                        collection.insertOne(newDocument);
                }
            }.runTaskAsynchronously(core);
        }
    }

    /**
     * Loads a given UUIDs timer data and applys it to the given player if they are online
     * @param uuid
     */
    public void loadTimerData(UUID uuid) {
        new BukkitRunnable() {
            public void run() {
                if(bastion == null)
                    bastion = MongoAPI.getCollection(core.getConfiguration().databaseName, "bastion");

                MongoCollection<Document> collection = bastion;
                FindIterable<Document> query;

                try {
                    query = MongoAPI.getQueryByFilter(collection, "uuid", uuid.toString());
                } catch (LinkageError err) {
                    loadTimerData(uuid);
                    return;
                }

                Document foundDocument = query.first();
                int tagDuration = 0, protectionDuration = 0, progressionDuration = 0;

                if(foundDocument != null) {
                    tagDuration = foundDocument.getInteger("tag");
                    protectionDuration = foundDocument.getInteger("protection");
                    progressionDuration = foundDocument.getInteger("progression");
                } else {
                    protectionDuration = core.getConfiguration().pvpProtDuration;
                    progressionDuration = core.getConfiguration().progressDuration;
                }

                FPlayer facPlayer = PlayerManager.getPlayer(uuid);

                if(tagDuration > 0)
                    facPlayer.addTimer(TimerManager.createTimer(TimerType.TAG, tagDuration));

                if(protectionDuration > 0 && core.getConfiguration().pvpProtEnabled)
                    facPlayer.addTimer(TimerManager.createTimer(TimerType.PVPPROT, protectionDuration));

                if(progressionDuration > 0 && core.getConfiguration().progressEnabled)
                    facPlayer.addTimer(TimerManager.createTimer(TimerType.PROGRESSION, progressionDuration));

                new BukkitRunnable() {
                    public void run() {
                        if(Bukkit.getPlayer(uuid) != null && facPlayer.isBeingTimed(TimerType.PROGRESSION)) {
                            if(Bukkit.getPlayer(uuid).hasPermission(Permissions.BYPASS_PROGRESSION))
                                facPlayer.removeTimer(TimerType.PROGRESSION);
                        }
                    }
                }.runTask(core);
            }
        }.runTaskAsynchronously(core);
    }

    public void onEnable() {
        loadListeners();
    }

    public void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new DatabaseListener(core), core);
    }

}
