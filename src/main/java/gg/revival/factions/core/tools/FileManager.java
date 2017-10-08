package gg.revival.factions.core.tools;

import gg.revival.factions.core.FC;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager {

    @Getter private FC core;
    private File configFile;
    private FileConfiguration configConfig;
    private File eventsFile;
    private FileConfiguration eventsConfig;

    public FileManager(FC core) {
        this.core = core;
    }

    void createFiles() {
        try {
            if (!core.getDataFolder().exists()) {
                core.getDataFolder().mkdirs();
            }

            configFile = new File(core.getDataFolder(), "config.yml");
            eventsFile = new File(core.getDataFolder(), "events.yml");

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                core.saveResource("config.yml", true);
            }

            if(!eventsFile.exists()) {
                eventsFile.getParentFile().mkdirs();
                core.saveResource("events.yml", true);
            }

            configConfig = new YamlConfiguration();
            eventsConfig = new YamlConfiguration();

            try {
                configConfig.load(configFile);
                eventsConfig.load(eventsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return configConfig;
    }
    public FileConfiguration getEvents() { return eventsConfig; }

    public void saveConfig() {
        try {
            configConfig.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveEvents() {
        try {
            eventsConfig.save(eventsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadFiles() {
        try {
            if (!core.getDataFolder().exists()) {
                core.getDataFolder().mkdirs();
            }

            configFile = new File(core.getDataFolder(), "config.yml");
            eventsFile = new File(core.getDataFolder(), "events.yml");

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                core.saveResource("config.yml", true);
            }

            if(!eventsFile.exists()) {
                eventsFile.getParentFile().mkdirs();
                core.saveResource("events.yml", true);
            }

            configConfig = new YamlConfiguration();
            eventsConfig = new YamlConfiguration();

            try {
                configConfig.load(configFile);
                eventsConfig.load(eventsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}