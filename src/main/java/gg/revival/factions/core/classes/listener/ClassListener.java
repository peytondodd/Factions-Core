package gg.revival.factions.core.classes.listener;

import gg.revival.factions.core.FC;
import gg.revival.factions.core.FactionManager;
import gg.revival.factions.core.classes.ClassProfile;
import gg.revival.factions.core.classes.ClassType;
import gg.revival.factions.core.classes.Classes;
import gg.revival.factions.core.classes.cont.RClass;
import gg.revival.factions.core.tools.Configuration;
import gg.revival.factions.core.tools.PlayerTools;
import gg.revival.factions.core.tools.TimeTools;
import gg.revival.factions.obj.Faction;
import gg.revival.factions.obj.PlayerFaction;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.UUID;

public class ClassListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            public void run() {
                ClassType foundClassType = Classes.getClassByArmor(player.getInventory().getHelmet(), player.getInventory().getChestplate(), player.getInventory().getLeggings(), player.getInventory().getBoots());

                if(foundClassType == null) return;

                RClass playerClass = Classes.getClassByClassType(foundClassType);

                for(PotionEffect passives : playerClass.getPassives()) {
                    if(player.hasPotionEffect(passives.getType()))
                        player.removePotionEffect(passives.getType());
                }

                Classes.createClassProfile(player.getUniqueId(), foundClassType);

            }
        }.runTaskLater(FC.getFactionsCore(), 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ClassProfile classProfile = Classes.getClassProfile(player.getUniqueId());

        if(classProfile == null) return;

        if(!classProfile.getConsumeCooldowns().isEmpty()) return;

        Classes.removeClassProfile(player.getUniqueId());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        if(!(damager instanceof Projectile)) return;

        Projectile projectile = (Projectile)damager;
        ProjectileSource source = projectile.getShooter();

        if(!(source instanceof Player)) return;

        Player shooter = (Player)source;
        ClassProfile classProfile = Classes.getClassProfile(shooter.getUniqueId());

        if(classProfile == null || !classProfile.getSelectedClass().equals(ClassType.ARCHER) || !classProfile.isActive()) return;

        Location flatDamagedLoc = damaged.getLocation(), flatDamagerLoc = shooter.getLocation();
        flatDamagedLoc.setY(0); flatDamagerLoc.setY(0);

        double distance = flatDamagedLoc.distance(flatDamagerLoc);
        double multiplier = 1.0;

        for(int i = 0; i < distance; i++)
            multiplier += 0.05;

        if(multiplier> Configuration.maxArcherDamage)
            multiplier = Configuration.maxArcherDamage;

        DecimalFormat format = new DecimalFormat("#.00");

        shooter.sendMessage(ChatColor.YELLOW + "[" + ChatColor.BLUE + "Archer" + ChatColor.YELLOW + " with " + ChatColor.BLUE + "Range" + ChatColor.YELLOW + "(" + ChatColor.RED + Math.round(distance) +
                ChatColor.YELLOW + ")]: Damage Increase (" + ChatColor.RED + format.format(multiplier) + "x" + ChatColor.YELLOW + " -> " +
                ChatColor.BLUE + Math.round(event.getDamage() * multiplier) + ChatColor.YELLOW + ")");

        event.setDamage(event.getDamage() * multiplier);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if(!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(player.getItemInHand() == null) return;

        ItemStack hand = player.getItemInHand();
        ClassProfile classProfile = Classes.getClassProfile(player.getUniqueId());

        if(classProfile.getSelectedClass() == null || !classProfile.isActive()) return;

        RClass playerClass = Classes.getClassByClassType(classProfile.getSelectedClass());

        if(playerClass.getActives().isEmpty()) return;

        boolean isConsumeable = false;

        for(Material consumeables : playerClass.getActives().keySet()) {
            if(hand.getType().equals(consumeables))
                isConsumeable = true;
        }

        if(!isConsumeable) return;

        Material consumeable = hand.getType();
        int cooldown = 0;

        if(consumeable.equals(Material.SUGAR))
            cooldown = Configuration.activeSpeedCooldown;
        else if(consumeable.equals(Material.FEATHER))
            cooldown = Configuration.activeJumpCooldown;
        else if(consumeable.equals(Material.GHAST_TEAR))
            cooldown = Configuration.activeRegenCooldown;
        else if(consumeable.equals(Material.BLAZE_POWDER))
            cooldown = Configuration.activeStrengthCooldown;

        if(classProfile.getConsumeCooldowns().containsKey(consumeable)) {
            long dur = classProfile.getConsumeCooldowns().get(consumeable) - System.currentTimeMillis();
            player.sendMessage(ChatColor.RED + "This active ability is locked for " + ChatColor.RED + "" + ChatColor.BOLD + TimeTools.getFormattedCooldown(true, dur) + ChatColor.RED + " seconds");
            return;
        }

        if(player.getItemInHand().getAmount() <= 1)
            player.setItemInHand(null);
        else
            player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);

        if(classProfile.getSelectedClass().equals(ClassType.BARD)) {
            Faction faction = FactionManager.getFactionByPlayer(player.getUniqueId());

            if(faction != null && faction instanceof PlayerFaction) {
                PlayerFaction playerFaction = (PlayerFaction)faction;

                classProfile.getConsumeCooldowns().put(consumeable, System.currentTimeMillis() + (cooldown * 1000L));

                new BukkitRunnable() {
                    public void run() {
                        classProfile.getConsumeCooldowns().remove(consumeable);

                        if(Bukkit.getPlayer(uuid) != null) {
                            Bukkit.getPlayer(uuid).sendMessage(ChatColor.GREEN + StringUtils.capitalize(playerClass.getActives().get(consumeable).getType().getName().replace("_", " ").toLowerCase()) + " has been unlocked");
                        } else {
                            if(classProfile.getConsumeCooldowns().isEmpty())
                                Classes.removeClassProfile(classProfile.getUuid());
                        }
                    }
                }.runTaskLater(FC.getFactionsCore(), cooldown * 20L);

                for(UUID nearby : PlayerTools.getNearbyFactionMembers(playerFaction, player.getLocation())) {
                    Player nearbyPlayer = Bukkit.getPlayer(nearby);

                    nearbyPlayer.removePotionEffect(playerClass.getActives().get(consumeable).getType());
                    nearbyPlayer.addPotionEffect(playerClass.getActives().get(consumeable));

                    new BukkitRunnable() {
                        public void run() {
                            if(nearbyPlayer == null) return;
                            if(Classes.getClassProfile(uuid) == null) return;

                            for(PotionEffect passives : Classes.getClassByClassType(Classes.getClassProfile(nearby).getSelectedClass()).getPassives()) {
                                if(passives.getType().equals(playerClass.getActives().get(consumeable).getType()))
                                    nearbyPlayer.addPotionEffect(passives);
                            }
                        }
                    }.runTaskLater(FC.getFactionsCore(), (playerClass.getActives().get(consumeable).getDuration() + 5L));

                    nearbyPlayer.sendMessage(ChatColor.YELLOW + "You now have " + ChatColor.BLUE +
                            StringUtils.capitalize(playerClass.getActives().get(consumeable).getType().getName().replace("_", " ").toLowerCase()) +
                            ChatColor.YELLOW + " for " +
                            ChatColor.GREEN + playerClass.getActives().get(consumeable).getDuration() / 20 + " seconds");
                }

                return;
            }
        }

        player.removePotionEffect(playerClass.getActives().get(consumeable).getType());
        player.addPotionEffect(playerClass.getActives().get(consumeable));

        new BukkitRunnable() {
            public void run() {
                if(player == null) return;
                if(!Classes.getClassProfile(uuid).getSelectedClass().equals(classProfile.getSelectedClass())) return;

                for(PotionEffect passives : playerClass.getPassives()) {
                    if(passives.getType().equals(playerClass.getActives().get(consumeable).getType()))
                        player.addPotionEffect(passives);
                }
            }
        }.runTaskLater(FC.getFactionsCore(), (playerClass.getActives().get(consumeable).getDuration() + 5L));

        player.sendMessage(ChatColor.YELLOW + "You now have " + ChatColor.BLUE +
                StringUtils.capitalize(playerClass.getActives().get(consumeable).getType().getName().replace("_", " ").toLowerCase()) +
                ChatColor.YELLOW + " for " +
                ChatColor.GREEN + playerClass.getActives().get(consumeable).getDuration() / 20 + " seconds");

        classProfile.getConsumeCooldowns().put(consumeable, System.currentTimeMillis() + (cooldown * 1000L));

        new BukkitRunnable() {
            public void run() {
                classProfile.getConsumeCooldowns().remove(consumeable);

                if(Bukkit.getPlayer(uuid) != null) {
                    Bukkit.getPlayer(uuid).sendMessage(ChatColor.GREEN + StringUtils.capitalize(playerClass.getActives().get(consumeable).getType().getName().replace("_", " ").toLowerCase()) + " has been unlocked");
                } else {
                    if(classProfile.getConsumeCooldowns().isEmpty())
                        Classes.removeClassProfile(classProfile.getUuid());
                }
            }
        }.runTaskLater(FC.getFactionsCore(), cooldown * 20L); // TODO: Update this with config value
    }

}
