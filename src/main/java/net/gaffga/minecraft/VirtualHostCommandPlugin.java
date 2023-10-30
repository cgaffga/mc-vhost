/*
 * Created on 29 Oct 2023
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.gaffga.minecraft;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author cgaffga
 *
 *         TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Generation -
 *         Code and Comments
 */
public class VirtualHostCommandPlugin extends JavaPlugin implements Listener {

    /**
     * vhost configs form config.yml.
     */
    protected Map<String, ConfigurationSection> vhostConfigs = new HashMap<String, ConfigurationSection>();

    /**
     * Map of login player name and their vhost (hostname they joined with).
     */
    protected Map<String, String> playerLoginHosts = new HashMap<String, String>();

    /**
     * Run when the plugin is loaded
     */
    @Override
    public void onEnable() {
        getLogger().info("enabled!");

        // register events from this plugin to be loaded...
        getServer().getPluginManager().registerEvents(this, this);

        // read config.yml...
        getLogger().info("Loding config from " + getDataFolder() + "/config.yml");
        File vhostConfigFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(vhostConfigFile);

        // go for all configured vhost sections...
        ConfigurationSection vhosts = yaml.getConfigurationSection("vhosts");
        if (vhosts != null) {
            for (String key : vhosts.getKeys(false)) {
                getLogger().info("vhost: " + key);

                ConfigurationSection hostConfig = vhosts.getConfigurationSection(key);

                // add to vhost configs...
                String hostname = hostConfig.getString("hostname");
                getLogger().info(" - hostname:" + hostname);
                vhostConfigs.put(hostname, hostConfig);

            } // NEXT vhost.
        }

    }

    /**
     * Catch event when player logs in. This is the event that provides us with the hostname the player used to join the server,
     * we keep that in our map.
     */
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        getLogger().info("Player " + player.getName() + " is logging in to host " + event.getHostname());

        // add player and hostname to map...
        playerLoginHosts.put(player.getName(), event.getHostname());
    }

    /**
     * Catch event when player finally joins. This is the point in time when we can run commands for the player.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String hostname = playerLoginHosts.get(player.getName());
        // hostname without port...
        String hostPart = hostname.substring(0, hostname.indexOf(':'));
        // just the port...
        String portPart = hostname.substring(hostname.indexOf(':') + 1);

        getLogger().info(
                "Player " + player.getName() + " is joining via host " + hostname + " (" + hostPart + " : " + portPart + ")");

        // find a matching vhost config...
        ConfigurationSection vhostConfig = null;
        for (String hostConfName : vhostConfigs.keySet()) {
            if (hostname.startsWith(hostConfName)) {
                vhostConfig = vhostConfigs.get(hostConfName);
            }
        }

        // if there is a config fir this host...
        if (vhostConfig != null) {
            getLogger().info("vhost config found.");
            // get the list of commands to run...
            List<String> commands = vhostConfig.getStringList("commands");

            // run all commands...
            for (String command : commands) {
                getLogger().info("run vhost commands: " + command);
                // replace placeholders...
                command = command.replace("%player%", player.getName());
                command = command.replace("%hostname%", hostPart);
                command = command.replace("%port%", portPart);
                getLogger().info(" -  vhost commands: " + command);

                // run command on console...
                getServer().dispatchCommand(getServer().getConsoleSender(), command);
            } // NEXT command.
        }
    }

}
