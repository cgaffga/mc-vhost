/*
 * Created on 29 Oct 2023
 *
 * Docs: https://github.com/cgaffga/mc-vhost/
 * License:  GNU GENERAL PUBLIC LICENSE Version 3
 */
package net.gaffga.minecraft;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.command.CommandException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

/**
 * Virtual Host Command Plugin for Bukkit/Minecraft Ever thought of having different domains/subdomains for the same
 * Minecraft/Bukkit server and depending on the domain a player joins offer different settings?<br>
 * This plugin is a bit like apache virtual hosts, detecting the host name of the Minecraft server the player used (CNAMEs).<br>
 * You can configure what commands to run on joining the server depending on the host name. E.g. you can have a server with
 * multiverse and subdomains hub.example.com and pvp.example.com and then send the player directly to the hub-world or the
 * arena-world depending on the domain they used to connect to the server.
 * 
 * @author cgaffga
 * @author TheMinePro
 * @see https://github.com/cgaffga/mc-vhost/
 */
public class VirtualHostCommandPlugin extends JavaPlugin implements Listener {

    public final static String CONFIG_VHOSTS = "vhosts";
    public final static String CONFIG_HOSTNAME = "hostname";
    public final static String CONIG_COMMANDS = "commands";
    public final static String CONIG_IF_IN_WORLD = "ifInWorld";
    public final static String CONIG_IF_NOT_IN_WORLD = "ifNotInWorld";
    public final static String CONIG_MOTD = "motd";
    public final static String CONIG_ICON = "icon";

    /**
     * vhost configs form config.yml.
     */
    protected final Map<String, ConfigurationSection> vhostConfigs = new LinkedHashMap<>();

    /**
     * Map of login player name and their vhost (hostname they joined with).
     */
    protected final Map<String, String> playerLoginHosts = new LinkedHashMap<>();

    /**
     * Run when the plugin is loaded
     */
    @Override
    public void onEnable() {
        getLogger().info("enabled!");
        // read config...
        reloadConfig();
        // register events from this plugin to be loaded...
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Run when the plugin is unloaded
     */
    @Override
    public void onDisable() {
        getLogger().info("disabled!");
        // housekeeping...
        this.vhostConfigs.clear();
    }

    /**
     * Reload the plugin config.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.vhostConfigs.clear();

        // read config.yml...
        getLogger().info("Loding config...");
        final Configuration config = getConfig();

        // go for all configured vhost sections...
        final ConfigurationSection vhosts = config.getConfigurationSection(CONFIG_VHOSTS);
        if (vhosts != null) {
            for (String key : vhosts.getKeys(false)) {
                getLogger().info("vhost: " + key);
                final ConfigurationSection hostConfig = vhosts.getConfigurationSection(key);

                // add to vhost configs...
                final String hostname = hostConfig.getString(CONFIG_HOSTNAME).toLowerCase();
                getLogger().info(" - hostname:" + hostname);
                this.vhostConfigs.put(hostname, hostConfig);

            } // NEXT vhost.
        }
    }

    /**
     * Catch event when player logs in. This is the event that provides us with the hostname the player used to join the server,
     * we keep that in our map.
     */
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        getLogger().info("Player " + player.getName() + " is logging in to host " + event.getHostname());

        // add player and hostname to map...
        this.playerLoginHosts.put(player.getName(), event.getHostname());
    }

    /**
     * Catch event when player finally joins. This is the point in time when we can run commands for the player. It runs with
     * 'highest' priority, aka the latest possible, so the players world is already present.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String hostname = this.playerLoginHosts.get(player.getName());
        // hostname without port...
        final String hostPart = hostname.substring(0, hostname.indexOf(':'));
        // just the port...
        final String portPart = hostname.substring(hostname.indexOf(':') + 1);
        // world name...
        final String world = player.getWorld().getName();

        getLogger().info("Player " + player.getName() + " is joining via host " + hostname + " to world " + world);

        // find a matching vhost config...
        for (String hostConfName : this.vhostConfigs.keySet()) {
            if (hostname.toLowerCase().startsWith(hostConfName)) {
                final ConfigurationSection vhostConfig = this.vhostConfigs.get(hostConfName);

                getLogger().info("vhost config found.");

                // check for 'ifInWorld' config...
                {
                    final String ifInWorld = vhostConfig.getString(CONIG_IF_IN_WORLD);
                    if (ifInWorld != null) {
                        try {
                            final Pattern regex = Pattern.compile(ifInWorld, Pattern.CASE_INSENSITIVE);
                            if (!regex.matcher(world).matches()) {
                                getLogger().info("Skipping commands for " + player.getName() + " as world " + world
                                        + " does match /" + CONIG_IF_IN_WORLD + "/='" + world + "'");
                                // ifInWorld does not match - skip rest...
                                continue;
                            }
                        } catch (PatternSyntaxException e) {
                            getLogger().log(Level.SEVERE, "Regular expression error on: " + ifInWorld, e);
                            continue;
                        }
                    }
                }

                // check for 'ifNotInWorld' config...
                {
                    final String ifNotInWorld = vhostConfig.getString(CONIG_IF_NOT_IN_WORLD);
                    if (ifNotInWorld != null) {
                        try {
                            final Pattern regex = Pattern.compile(ifNotInWorld, Pattern.CASE_INSENSITIVE);
                            if (regex.matcher(world).matches()) {
                                getLogger().info("Skipping commands for " + player.getName() + " as world " + world
                                        + " does not match /" + CONIG_IF_NOT_IN_WORLD + "/='" + world + "'");
                                // ifNotInWorld does match - skip rest...
                                continue;
                            }
                        } catch (PatternSyntaxException e) {
                            getLogger().log(Level.SEVERE, "Regular expression error on: " + ifNotInWorld, e);
                            continue;
                        }
                    }
                }

                try {
                    // run all commands...
                    for (String command : vhostConfig.getStringList(CONIG_COMMANDS)) {
                        getLogger().info("run vhost commands: " + command);
                        // replace placeholders...
                        command = command.replace("%player%", player.getName());
                        command = command.replace("%hostname%", hostPart);
                        command = command.replace("%port%", portPart);
                        getLogger().info(" -  vhost commands: " + command);

                        // run command on console...
                        getServer().dispatchCommand(getServer().getConsoleSender(), command);
                    } // NEXT command.
                } catch (CommandException e) {
                    getLogger().log(Level.SEVERE,
                            "Could not execute command for player " + player.getName() + " for vhost config " + hostConfName,
                            e);
                }
            }
        } // NEXT vhostConfig.
    }

    /**
     * Catch the event for players leaving the server, release hostname for the player leaving.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        getLogger().info("Player " + event.getPlayer().getName() + " is leaving");
        // housekeeping...
        this.playerLoginHosts.remove(event.getPlayer().getName());
    }

    /**
     * Return per-vhost MOTD and server icon. This only works if your server implementation returns a hostname in
     * ServerListPingEvent, many don't :( For paper-mc we have implemented a workaround to get the hostname from
     * StandardPaperServerListPingEventImpl.
     */
    @EventHandler
    public void onPing(ServerListPingEvent event) {
        String hostname = event.getHostname();
        // getLogger().info("Hostname Ping (1): '" + hostname + "'");

        // workaround for paper-mc servers, get the hostname via reflection...
        getLogger().info(event.getClass().getCanonicalName());
        if ((hostname == null || hostname.isBlank()) && "com.destroystokyo.paper.network.StandardPaperServerListPingEventImpl"
                .equals(event.getClass().getCanonicalName())) {
            getLogger().info("Workaround for paper-mc server - get virtual hostname for ping...");
            try {
                Object status = event.getClass().getMethod("getClient").invoke(event);
                InetSocketAddress virtualHost = (InetSocketAddress) status.getClass().getMethod("getVirtualHost")
                        .invoke(status);
                hostname = virtualHost.getHostString();
                // getLogger().info("Hostname Ping (2): '" + hostname + "'");

            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Method getClient problem!", e);
            }
        }

        // find a matching vhost config...
        for (String hostConfName : this.vhostConfigs.keySet()) {
            if (hostname.toLowerCase().startsWith(hostConfName)) {
                final ConfigurationSection vhostConfig = this.vhostConfigs.get(hostConfName);

                // MOTD message for this vhost server...
                final String motd = vhostConfig.getString(CONIG_MOTD);
                if (motd != null) {
                    getLogger().info("server ping event: vhost motd found for " + hostname + ": " + motd);
                    event.setMotd(motd);
                }

                // server icon for this vhost server...
                final String iconFilename = vhostConfig.getString(CONIG_ICON);
                if (iconFilename != null) {
                    getLogger().info("server ping event: vhost icon found for " + hostname + ": " + iconFilename);
                    try {
                        CachedServerIcon icon = getServer().loadServerIcon(new File(iconFilename));
                        event.setServerIcon(icon);
                    } catch (Exception e) {
                        getLogger().log(Level.SEVERE, "Could not load server icon " + iconFilename + " for vhost " + hostname,
                                e);
                    }
                }

            }
        } // NEXT vhostConfig.
    }

}
