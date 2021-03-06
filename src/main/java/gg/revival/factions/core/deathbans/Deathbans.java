package gg.revival.factions.core.deathbans;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import gg.revival.driver.MongoAPI;
import gg.revival.factions.claims.Claim;
import gg.revival.factions.core.FC;
import gg.revival.factions.core.FactionManager;
import gg.revival.factions.core.deathbans.callbacks.ActiveDeathbanCallback;
import gg.revival.factions.core.deathbans.callbacks.DeathbanCallback;
import gg.revival.factions.core.deathbans.callbacks.DeathbanDurationCallback;
import gg.revival.factions.core.deathbans.command.DeathbanCommand;
import gg.revival.factions.core.deathbans.command.DeathsCommand;
import gg.revival.factions.core.deathbans.listener.DeathbanListener;
import gg.revival.factions.core.events.obj.Event;
import gg.revival.factions.obj.ServerFaction;
import lombok.Getter;
import mkremins.fanciful.FancyMessage;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class Deathbans {

    @Getter private FC core;
    @Getter public DeathMessages deathMessages;

    public Deathbans(FC core) {
        this.core = core;
        this.deathMessages = new DeathMessages();

        onEnable();
    }

    /**
     * Returns a Death object if the given UUID has an active deathban
     * @param uuid
     * @return
     */
    public void getActiveDeathban(UUID uuid, ActiveDeathbanCallback callback) {
        new BukkitRunnable() {
            public void run() {
                if(core.getDatabaseManager().getDeathbans() == null)
                    core.getDatabaseManager().setDeathbans(MongoAPI.getCollection(core.getConfiguration().databaseName, "deathbans"));

                MongoCollection collection = core.getDatabaseManager().getDeathbans();
                FindIterable<Document> query = null;

                try {
                    query = MongoAPI.getQueryByFilter(collection, "killed", uuid.toString());
                } catch (LinkageError err) {
                    getActiveDeathban(uuid, callback);
                    return;
                }

                for (Document current : query) {
                    if (current.getLong("expires") > System.currentTimeMillis()) {
                        UUID deathId = UUID.fromString(current.getString("uuid"));
                        String reason = current.getString("reason");
                        long created = current.getLong("created");
                        long expires = current.getLong("expires");

                        Death death = new Death(deathId, uuid, reason, created, expires);

                        new BukkitRunnable() {
                            public void run() {
                                callback.onQueryDone(death);
                            }
                        }.runTask(core);

                        return;
                    }
                }

                new BukkitRunnable() {
                    public void run() {
                        callback.onQueryDone(null);
                    }
                }.runTask(core);
            }
        }.runTaskAsynchronously(core);
    }

    /**
     * Returnns a set of Deaths found by a users UUID
     * @param uuid
     * @param callback
     */
    public void getDeathsByUUID(UUID uuid, DeathbanCallback callback) {
        if(!MongoAPI.isConnected()) {
            Set<Death> emptyResult = new HashSet<>();
            callback.onQueryDone(emptyResult);
        }

        new BukkitRunnable() {
            public void run() {
                Set<Death> result = new HashSet<>();

                MongoCollection collection = core.getDatabaseManager().getDeathbans();
                FindIterable<Document> query = null;

                try {
                    query = MongoAPI.getQueryByFilter(collection, "killed", uuid.toString());
                } catch (LinkageError err) {
                    getDeathsByUUID(uuid, callback);
                    return;
                }

                Iterator<Document> iterator = query.iterator();

                while(true) {
                    if (!(iterator.hasNext())) break;
                    Document current = iterator.next();

                    UUID uuid = UUID.fromString(current.getString("uuid"));
                    UUID killed = UUID.fromString(current.getString("killed"));
                    String reason = current.getString("reason");
                    long created = current.getLong("created");
                    long expires = current.getLong("expires");

                    Death death = new Death(uuid, killed, reason, created, expires);

                    result.add(death);
                }

                new BukkitRunnable() {
                    public void run() {
                        callback.onQueryDone(result);
                    }
                }.runTask(core);
            }
        }.runTaskAsynchronously(core);
    }

    /**
     * Saves a Death object to DB
     * @param death
     */
    public void saveDeathban(Death death) {
        new BukkitRunnable() {
            public void run() {
                if(core.getDatabaseManager().getDeathbans() == null)
                    core.getDatabaseManager().setDeathbans(MongoAPI.getCollection(core.getConfiguration().databaseName, "deathbans"));

                MongoCollection collection = core.getDatabaseManager().getDeathbans();
                FindIterable<Document> query = null;

                try {
                    query = MongoAPI.getQueryByFilter(collection, "uuid", death.getUuid().toString());
                } catch (LinkageError err) {
                    saveDeathban(death);
                    return;
                }

                Document document = query.first();

                Document newDoc = new Document("uuid", death.getUuid().toString())
                        .append("killed", death.getKilled().toString())
                        .append("reason", death.getReason())
                        .append("created", death.getCreatedTime())
                        .append("expires", death.getExpiresTime());

                if(document != null) {
                    collection.replaceOne(document, newDoc);
                }

                else {
                    collection.insertOne(newDoc);
                }

            }
        }.runTaskAsynchronously(core);
    }

    /**
     * Deathban and kick a player for a given duration and reason
     * @param uuid
     * @param reason
     * @param duration
     */
    public void deathbanPlayer(UUID uuid, String reason, long duration) {
        UUID dbID = UUID.randomUUID();
        long created = System.currentTimeMillis();
        long expires = System.currentTimeMillis() + duration;

        Death death = new Death(dbID, uuid, reason, created, expires);

        saveDeathban(death);

        if(Bukkit.getPlayer(uuid) != null)
            Bukkit.getPlayer(uuid).kickPlayer(getDeathbanMessage(death));
    }

    /**
     * Determines a Deathban duration based on the location of the death. Callsback when the result is finished
     * @param uuid
     * @param location
     * @param callback
     */
    public void getDeathbanDurationByLocation(UUID uuid, Location location, DeathbanDurationCallback callback) {
        core.getStats().getStats(uuid, stats -> {
            long deathbanDuration = stats.getCurrentPlaytime();

            for(Event event : core.getEvents().getEventManager().getActiveEvents()) {
                if(event.getHookedFactionId() == null || FactionManager.getFactionByUUID(event.getHookedFactionId()) == null) continue;

                ServerFaction serverFaction = (ServerFaction)FactionManager.getFactionByUUID(event.getHookedFactionId());

                if(serverFaction== null || serverFaction.getClaims().isEmpty()) continue;

                for(Claim claim : serverFaction.getClaims()) {
                    if(!claim.inside(location, true)) continue;

                    if(deathbanDuration > core.getConfiguration().eventDeathban)
                        deathbanDuration = core.getConfiguration().eventDeathban;
                }
            }

            if(deathbanDuration > core.getConfiguration().normalDeathban)
                deathbanDuration = core.getConfiguration().normalDeathban;

            callback.onLookupComplete(deathbanDuration);
        });
    }

    /**
     * Gets deathban kick message based on active deathban
     * @param death
     * @return
     */
    public String getDeathbanMessage(Death death) {
        long duration = death.getExpiresTime() - System.currentTimeMillis();

        int seconds = (int) (duration / 1000) % 60;
        int minutes = (int) (duration / (1000 * 60) % 60);
        int hours   = (int) (duration / (1000 * 60 * 60) % 24);

        if(seconds >= 60)
            seconds = 59;

        StringBuilder time = new StringBuilder();

        if(hours > 0)
            time.append(hours + " hours ");

        if(minutes > 0)
            time.append(minutes + " minutes ");

        if(seconds > 0) {
            if(time.toString().length() > 0)
                time.append("and " + seconds + " seconds ");
            else
                time.append(seconds + " seconds ");
        }

        return ChatColor.RED + "You are deathbanned for another " + time.toString().trim() + "." + "\n" +
                ChatColor.RED + " You can bypass this deathban by using a life. Purchase lives at " + ChatColor.AQUA + "http://hcfrevival.net/store" + ChatColor.RESET + ".";
    }

    /**
     * Sends an organized list of deathbans based on a UUID
     * @param player
     * @param username
     * @param deaths
     */
    public void sendDeathbans(Player player, String username, Set<Death> deaths) {
        player.sendMessage(ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "-----------------------------------");
        player.sendMessage(username + ChatColor.BOLD + " | " + ChatColor.GREEN + deaths.size() + " deaths on record");
        player.sendMessage(ChatColor.YELLOW + "Hover over each death to view more information");
        player.sendMessage("     ");

        SimpleDateFormat formatter = new SimpleDateFormat("M-d-yyyy '@' hh:mm:ss a");

        int cursor = 1;

        for(Death death : deaths) {
            if(cursor >= 10) break;

            Date create = new Date(death.getCreatedTime());
            Date expire = new Date(death.getExpiresTime());
            List<String> info = new ArrayList<>();

            info.add(ChatColor.RED + "Died" + ChatColor.WHITE + ": " + formatter.format(create));
            info.add(ChatColor.DARK_AQUA + "Expires" + ChatColor.WHITE + ": " + formatter.format(expire));

            new FancyMessage(" - ").color(ChatColor.GOLD).then(death.getReason()).color(ChatColor.YELLOW).tooltip(info).send(player);

            cursor++;
        }

        player.sendMessage(ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "-----------------------------------");
    }

    public void onEnable() {
        loadCommands();
        loadListeners();
        loadCommands();
    }

    private void loadCommands() {
        core.getCommand("deathban").setExecutor(new DeathbanCommand(core));
        core.getCommand("deaths").setExecutor(new DeathsCommand(core));
    }

    private void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new DeathbanListener(core), core);
    }

}
