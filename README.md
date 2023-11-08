
# Virtual Host Command Plugin for Bukkit/Minecraft
Ever thought of having different domains/subdomains for the same Minecraft/Bukkit server and depending on the domain a player joins offer different settings?
This plugin is a bit like apache virtual hosts, detecting the host name of the Minecraft server the player used (CNAMEs). You can configure what commands to run on joining the server depending on the host name. E.g. you can have a server with multiverse and subdomains hub.example.com and pvp.example.com and then send the player directly to the hub-world or the arena-world depending on the domain they used to connect to the server.
> **Note:** Bedrock proxy with **geyser** does not supply the host name on join – so it's not working for vhosts. It only supplies the IP address, so you need a different IP address for each domain connecting via geyser/proxy.
> **FIX** add the following to you geyser config.yml:
 
      # Forward the hostname that the Bedrock client used to connect over to the Java server
      # This is designed to be used for forced hosts on proxies
      forward-hostname: true

> **Note:** We heavily rely on the supplied hostname. This is not always given in PingServerList list event, so **motd** and **icon** per vhost does not always work on all server implementations. For paper-mc we added a workaround to get the hostname used for the ping.

Here is an example of four vhosts running on the same server with the same IP address but accessed using different vhosts via different CNAMEs:

![Example Minecraft Server List](docs/example-server-list.png?raw=true "Example Minecraft Server List")

### Update 0.9.2
 - minor fixes, cleanup, housekeeping
 - added *ifInWorld* and *ifNotInWorld* conditions (see example config)

### Update 0.9.3
 - small fixes
 - added *motd* and *icon* config for vhosts (see example config)

## Example Config
see up2date file in *plugins/VirtualHostCommandPlugin/config.yml*

# example config file for VirtualHostCommand bukkit plugin
# Location: plugins/VirtualHostCommandPlugin/config.yml
    vhosts:
      host_example:
        # the hoostname is matched agains the joining hostname via starts-with, so parts work as well
        # you can also add the port, sub.domain.com:port
        # it's mandatory, and always case-insensitive
        hostname: example.com
        # (optional) only run if player joined this world(s) 
        ifInWorld: "world_some_regex"
        # (optional) only run if player did not join this world(s)
        # if ifInWorld and ifNotInWorld present, they must be both true
        ifNotInWorld: "world_some_regex"
        # (optional) MOTD for this vhost
        # NOTE: this only works if you server implementation returns a hostname in the ping event, many don't :(
        #       for  paper-mc we have added a workaround to fetch the vhost hostname pinged via reflection.
        motd: "some colorfull MOTD server message\nmultiline if you like!"
        # (optional) server icon filename, format like server-icon.png, same root directory
        # NOTE: same restrictions as for 'motd' config option apply 
        icon: server-icon-example.png 
        # List of commands to be executed if joining via this hostname    
        commands:
          # he following placeholders are allowd:
          # %player% , %hostname% and %port%
          - some command ...
          - another cmmand with %player% to replace with player name
          - msg %player% haha it works from %hostname%
    
      host_example2:
        # example with an IP
        hostname: "123.45.67.89"
        commands:
          - some command ...
          - another cmmand with %player% to replace with player name
          - msg %player% haha it works
    
      host_hub:
        hostname: hub.myworld.com
        ifNotInWorld: "^world_hub"
        commands:
          - gamemode %player% survival
          - msg %player% crazy shit
          - mv tp %player% world_hub
    
see https://github.com/cgaffga/mc-vhost/tree/main/docs


## Source
Source code can be found at https://github.com/cgaffga/mc-vhost/

## Download
Builds in https://github.com/cgaffga/mc-vhost/tree/main/dist/

or on Spigot https://www.spigotmc.org/resources/virtualhostcommandplugin.113311/
