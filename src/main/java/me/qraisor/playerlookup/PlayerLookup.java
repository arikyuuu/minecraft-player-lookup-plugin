package me.qraisor.playerlookup;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import lombok.Getter;

import me.qraisor.playerlookup.utils.Database;

public class PlayerLookup extends JavaPlugin {

    @Getter
    private static PlayerLookup instance;

    private static final Logger mcLogger = Logger.getLogger("Minecraft");
    private static final String configFile = "config.yml";

    private boolean logging;

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getServer().getPluginManager().registerEvents(new LogListener(), this);
        loadConfig();
        if (!Database.establishConn()) {
            mcLogger.log(Level.SEVERE, "[PlayerLookup] Error connecting to the database");
            getPluginLoader().disablePlugin(this);
            return;
        }
        mcLogger.log(Level.FINE, "[PlayerLookup] Connected to the database");
    }

    private class LogListener implements Listener {
        @EventHandler
        public void onPlayerLogoff(PlayerQuitEvent e) {
            if(logging) { mcLogger.log(Level.INFO, "[PlayerLookup] (Left) "+e.getPlayer().getName()); }
            if (Database.updatePlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName())) {
                if(logging) { mcLogger.log(Level.INFO, "[PlayerLookup] (Leave) Updated "+e.getPlayer().getName()); }
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent e) {
            if(logging) { mcLogger.log(Level.INFO, "[PlayerLookup] (Joined) "+e.getPlayer().getName()); }
            if (Database.newPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName())) {
                if(logging) { mcLogger.log(Level.INFO, "[PlayerLookup] (Join) Updated "+e.getPlayer().getName()); }
            }
        }
    }

    public void log(String msg) {
        mcLogger.log(Level.INFO, msg);
    }

    private void loadConfig() {
        File sourceDir = getDataFolder();

        if (!sourceDir.exists())
            sourceDir.mkdir();

        FileConfiguration config = new YamlConfiguration();
        try {
            mcLogger.log(Level.INFO, "[PlayerLookup] Config successfully loaded");
            config.load(new File(sourceDir, configFile));
            logging = config.getBoolean("logging");
        } catch (FileNotFoundException ex) {
            //set defaults in the config
            config.set("mysql.host", "");
            config.set("mysql.port", "");
            config.set("mysql.database", "");
            config.set("mysql.username", "");
            config.set("mysql.password", "");
            config.set("mysql.ssl", "");
            config.set("logging", false);

            //write config to file
            persistConfig(config);
        } catch (IOException | InvalidConfigurationException ex) {
            mcLogger.log(Level.SEVERE, "[PlayerLookup] Error loading config");
        }
    }

    private void persistConfig(FileConfiguration config) {
        try {
            config.save(new File(getDataFolder(), configFile));
            mcLogger.log(Level.INFO, "[PlayerLookup] Default config loaded, please restart the server");
        } catch (IOException ex1) {
            mcLogger.log(Level.SEVERE, "[PlayerLookup] Error writing to config");
        }
    }
}
