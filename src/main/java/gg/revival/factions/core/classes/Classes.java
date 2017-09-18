package gg.revival.factions.core.classes;

import com.google.common.collect.Sets;
import gg.revival.factions.core.FC;
import gg.revival.factions.core.PlayerManager;
import gg.revival.factions.core.classes.cont.RClass;
import gg.revival.factions.core.classes.listener.ArmorEventsListener;
import gg.revival.factions.core.classes.listener.ClassListener;
import gg.revival.factions.core.tools.Configuration;
import gg.revival.factions.core.tools.armorevents.ArmorListener;
import gg.revival.factions.obj.FPlayer;
import gg.revival.factions.timers.TimerManager;
import gg.revival.factions.timers.TimerType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Set;
import java.util.UUID;

public class Classes {

    @Getter static Set<ClassProfile> activeClasses = Sets.newHashSet();
    @Getter static Set<RClass> enabledClasses = Sets.newHashSet();

    public static RClass getClassByClassType(ClassType type) {
        for(RClass classes : enabledClasses)
            if(classes.getType().equals(type)) return classes;

        return null;
    }

    public static ClassProfile getClassProfile(UUID uuid) {
        for(ClassProfile loadedClasses : activeClasses)
            if(loadedClasses.getUuid().equals(uuid)) return loadedClasses;

        return null;
    }

    public static void createClassProfile(UUID uuid, ClassType classType) {
        ClassProfile profile = getClassProfile(uuid);
        FPlayer facPlayer = PlayerManager.getPlayer(uuid);

        if(profile == null)
            profile = new ClassProfile(uuid);

        facPlayer.addTimer(TimerManager.createTimer(TimerType.CLASS, Configuration.classWarmupDelay));
        profile.setSelectedClass(classType);
        profile.setActive(false);

        activeClasses.add(profile);

        if(Bukkit.getPlayer(uuid) != null)
            Bukkit.getPlayer(uuid).sendMessage(ChatColor.BLUE + StringUtils.capitalize(classType.toString().toLowerCase()) + ChatColor.YELLOW + " is now " + ChatColor.GOLD + "warming up");
    }

    public static void removeClassProfile(UUID uuid) {
        ClassProfile profile = getClassProfile(uuid);
        FPlayer facPlayer = PlayerManager.getPlayer(uuid);

        if(profile != null)
            activeClasses.remove(profile);

        if(facPlayer != null) {
            if(facPlayer.isBeingTimed(TimerType.CLASS))
                facPlayer.removeTimer(TimerType.CLASS);
        }
    }

    public static void addToClass(Player player, ClassType type) {
        RClass classToGive = getClassByClassType(type);
        ClassProfile classProfile = getClassProfile(player.getUniqueId());

        for(PotionEffect effectsToGive : classToGive.getPassives()) {
            player.removePotionEffect(effectsToGive.getType());
            player.addPotionEffect(effectsToGive);
        }

        classProfile.setActive(true);

        player.sendMessage(ChatColor.BLUE + StringUtils.capitalize(type.toString().toLowerCase()) + ChatColor.YELLOW + " is now " + ChatColor.GREEN + "ready");
    }

    public static void removeFromClass(UUID uuid) {
        ClassProfile profile = getClassProfile(uuid);
        RClass classToRemove = getClassByClassType(profile.getSelectedClass());

        for(PotionEffect effectsToRemove : classToRemove.getPassives()) {
            if(Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).hasPotionEffect(effectsToRemove.getType()))
                Bukkit.getPlayer(uuid).removePotionEffect(effectsToRemove.getType());
        }

        if(Bukkit.getPlayer(uuid) != null)
            Bukkit.getPlayer(uuid).sendMessage(ChatColor.BLUE + StringUtils.capitalize(classToRemove.getType().toString().toLowerCase()) + ChatColor.YELLOW + " is now " + ChatColor.RED + "disabled");

        removeClassProfile(uuid);
    }

    public static ClassType getClassByArmor(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        if(helmet == null || chestplate == null || leggings == null || boots == null) return null;

        if(
                helmet.getType().equals(Material.LEATHER_HELMET) &&
                chestplate.getType().equals(Material.LEATHER_CHESTPLATE) &&
                leggings.getType().equals(Material.LEATHER_LEGGINGS) &&
                boots.getType().equals(Material.LEATHER_BOOTS)) {

            return ClassType.ARCHER;
        }

        if(
                helmet.getType().equals(Material.CHAINMAIL_HELMET) &&
                        chestplate.getType().equals(Material.CHAINMAIL_CHESTPLATE) &&
                        leggings.getType().equals(Material.CHAINMAIL_LEGGINGS) &&
                        boots.getType().equals(Material.CHAINMAIL_BOOTS)) {

            return ClassType.SCOUT;
        }

        if(
                helmet.getType().equals(Material.IRON_HELMET) &&
                        chestplate.getType().equals(Material.IRON_CHESTPLATE) &&
                        leggings.getType().equals(Material.IRON_LEGGINGS) &&
                        boots.getType().equals(Material.IRON_BOOTS)) {

            return ClassType.MINER;
        }

        if(
                helmet.getType().equals(Material.GOLD_HELMET) &&
                        chestplate.getType().equals(Material.GOLD_CHESTPLATE) &&
                        leggings.getType().equals(Material.GOLD_LEGGINGS) &&
                        boots.getType().equals(Material.GOLD_BOOTS)) {

            return ClassType.BARD;
        }

        return null;
    }

    public static void onEnable() {
        loadListeners();
    }

    public static void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new ArmorListener(), FC.getFactionsCore());
        Bukkit.getPluginManager().registerEvents(new ArmorEventsListener(), FC.getFactionsCore());
        Bukkit.getPluginManager().registerEvents(new ClassListener(), FC.getFactionsCore());
    }

}
