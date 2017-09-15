package gg.revival.factions.core.locations.command;
/*
** John @ 9/13/2017
*/

import gg.revival.factions.core.locations.Locations;
import gg.revival.factions.core.tools.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if(!command.getName().equalsIgnoreCase("end")) return false;

        if(!(sender instanceof Player)) {
            sender.sendMessage("This command can not be ran by console");
            return false;
        }

        Player player = (Player)sender;

        if(args.length == 0) {
            if(!player.hasPermission(Permissions.CORE_MOD) && !player.hasPermission(Permissions.CORE_ADMIN)) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command");
                return false;
            }

            player.teleport(Locations.getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "Teleported to The End");
            return false;
        }

        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("set")) {
                if(!player.hasPermission(Permissions.CORE_ADMIN)) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command");
                    return false;
                }

                Locations.setEndSpawnLocation(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "End Spawn location has been updated");
                return false;
            }
        }

        return false;
    }

}