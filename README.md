
# Virtual Host Command Plugin for Bukkit/Minecraft
Ever thought of having different domains/subdomains for the same Minecraft/Bukkit server and depending on the domain a player joins offer different settings?
This plugin is a bit like apache virtual hosts, detecting the host name of the Minecraft server the player used (CNAMEs). You can configure what commands to run on joining the server depending on the host name. E.g. you can have a server with multiverse and subdomains hub.example.com and pvp.example.com and then send the player directly to the hub-world or the arena-world depending on the domain they used to connect to the server.
> **Note:** Bedrock proxy with **geyser** does not supply the host name on join – so it's not working for vhosts. It only supplies the IP address, so you need a different IP address for each domain connecting via geyser/proxy.
> **FIX** add the following to you geyser config.yml:
 
      # Forward the hostname that the Bedrock client used to connect over to the Java server
      # This is designed to be used for forced hosts on proxies
      forward-hostname: true


## Example Config
file in *plugins/VirtualHostCommandPlugin/config.yml*

    # example config file for VirtualHostCommand bukkit plugin
    # Location: plugins/VirtualHostCommandPlugin/config.yml
    vhosts:
      host_example:
        # the hoostname is matched agains the joining hostname via starts-with, so parts work as well
        # you can also add the port, sub.domain.com:port
        hostname: example.com
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
        commands:
          - gamemode %player% survival
          - msg %player% crazy shit
          - mv tp %player% hub
          
see https://github.com/cgaffga/mc-vhost/tree/main/docs


## Source
Source code can be found at https://github.com/cgaffga/mc-vhost/

## Download
Builds in https://github.com/cgaffga/mc-vhost/tree/main/dist/

or on Spigot https://www.spigotmc.org/resources/virtualhostcommandplugin.113311/

