package gg.revival.factions.core;

import gg.revival.factions.core.bastion.Bastion;
import gg.revival.factions.core.classes.Classes;
import gg.revival.factions.core.deathbans.Deathbans;
import gg.revival.factions.core.events.Events;
import gg.revival.factions.core.limits.Limiter;
import gg.revival.factions.core.lives.Lives;
import gg.revival.factions.core.mechanics.Mechanics;
import gg.revival.factions.core.mining.Mining;
import gg.revival.factions.core.progression.Progression;
import gg.revival.factions.core.servermode.ServerMode;
import gg.revival.factions.core.signs.Signs;
import gg.revival.factions.core.stats.StatsManager;
import gg.revival.factions.core.tools.Configuration;
import gg.revival.factions.core.ui.UI;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class FC extends JavaPlugin {

    @Getter static FC factionsCore;

    @Override
    public void onEnable() {
        factionsCore = this;

        Configuration.load();

        Bastion.onEnable();
        Classes.onEnable();
        Lives.onEnable();
        Limiter.onEnable();
        Mechanics.onEnable();
        Mining.onEnable();
        Progression.onEnable();
        ServerMode.onEnable();
        Signs.onEnable();
        Events.onEnable();
        Deathbans.onEnable();
        StatsManager.onEnable();
        UI.onEnable();
    }

    @Override
    public void onDisable() {
        Bastion.onDisable();
        StatsManager.onDisable();
        Classes.onDisable();
        Progression.onDisable();
        ServerMode.onDisable();

        factionsCore = null;
    }
}
