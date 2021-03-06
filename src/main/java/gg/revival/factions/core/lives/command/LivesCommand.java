package gg.revival.factions.core.lives.command;

import gg.revival.factions.core.FC;
import gg.revival.factions.core.tools.Permissions;
import lombok.Getter;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LivesCommand implements CommandExecutor {

    @Getter private FC core;

    public LivesCommand(FC core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if(!command.getName().equalsIgnoreCase("lives")) return false;

        if(args.length == 0) {
            if(sender instanceof Player) {
                Player player = (Player)sender;
                core.getLives().getLives(player.getUniqueId(), lives -> player.sendMessage(ChatColor.DARK_GREEN + "Your lives" + ChatColor.WHITE + ": " + lives));
            }

            return false;
        }

        if(args.length == 1) {
            String namedPlayer = args[0];

            core.getOfflinePlayerLookup().getOfflinePlayerByName(namedPlayer, (uuid, username) -> {
                if(uuid == null || username == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found");
                    return;
                }

                core.getLives().getLives(uuid, lives -> sender.sendMessage(ChatColor.DARK_GREEN + username + "'s lives" + ChatColor.WHITE + ": " + lives));
            });

            return false;
        }

        if(args.length == 2) {
            sender.sendMessage(ChatColor.RED + "/lives" + "\n" + ChatColor.RED + "/lives [player]" + "\n" + ChatColor.RED + "/lives give <player> <amt>");

            if(sender instanceof Player) {
                Player player = (Player)sender;

                if(player.hasPermission(Permissions.CORE_ADMIN)) {
                    player.sendMessage(ChatColor.RED + "/lives set <player> <amt>");
                }
            }

            return false;
        }

        if(args.length == 3) {
            if(args[0].equalsIgnoreCase("give")) {
                String namedPlayer = args[1];
                String namedAmount = args[2];

                if(!NumberUtils.isNumber(namedAmount)) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount");
                    return false;
                }

                int givenLives = NumberUtils.toInt(namedAmount);

                if(sender instanceof Player) {
                    Player sendingPlayer = (Player)sender;

                    core.getOfflinePlayerLookup().getOfflinePlayerByName(namedPlayer, (uuid, username) -> {
                        if(uuid == null || username == null) {
                            sendingPlayer.sendMessage(ChatColor.RED + "Player not found");
                            return;
                        }

                        if(sendingPlayer.getUniqueId().equals(uuid)) {
                            sendingPlayer.sendMessage(ChatColor.RED + "You can not send lives to yourself");
                            return;
                        }

                        core.getLives().getLives(sendingPlayer.getUniqueId(), sendingLives -> {
                            if(sendingLives == 0 || sendingLives < givenLives) {
                                sendingPlayer.sendMessage(ChatColor.RED + "You do not have enough lives to perform this task");
                                return;
                            }

                            core.getLives().getLives(uuid, receivingLives -> core.getLives().setLives(sendingPlayer.getUniqueId(), sendingLives - givenLives, (sendingUUID, newSendingLives) -> core.getLives().setLives(uuid, receivingLives + givenLives, (receivedUUID, newReceivingLives) -> {
                                if(Bukkit.getPlayer(sendingUUID) != null) {
                                    Bukkit.getPlayer(sendingUUID).sendMessage(ChatColor.GREEN + "You sent " + givenLives + " lives to " + username);
                                    Bukkit.getPlayer(sendingUUID).sendMessage(ChatColor.GREEN + "You now have " + newSendingLives + " lives");
                                }

                                if(Bukkit.getPlayer(receivedUUID) != null) {
                                    if(Bukkit.getPlayer(sendingUUID) != null) {
                                        Bukkit.getPlayer(receivedUUID).sendMessage(ChatColor.GREEN + Bukkit.getPlayer(sendingUUID).getName() + " sent you " + givenLives + " lives");
                                    } else {
                                        Bukkit.getPlayer(receivedUUID).sendMessage(ChatColor.GREEN + "" + givenLives + " lives have been added to your account");
                                    }

                                    Bukkit.getPlayer(receivedUUID).sendMessage(ChatColor.GREEN + "You now have " + newReceivingLives + " lives");
                                }

                                core.getLog().log(sendingPlayer.getName() + " gave " + givenLives + " lives to " + username);
                            })));
                        });
                    });

                    return false;
                }

                core.getOfflinePlayerLookup().getOfflinePlayerByName(namedPlayer, (uuid, username) -> {
                    if(uuid == null || username == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return;
                    }

                    core.getLives().getLives(uuid, lives -> core.getLives().setLives(uuid, lives + givenLives, (updatedUuid, newLives) -> {
                        if(updatedUuid != null && Bukkit.getPlayer(updatedUuid) != null) {
                            Bukkit.getPlayer(updatedUuid).sendMessage(ChatColor.GREEN + "" + givenLives + " lives have been added to your account");
                            Bukkit.getPlayer(updatedUuid).sendMessage(ChatColor.GREEN + "You now have " + newLives + " lives");
                        }

                        core.getLog().log(givenLives + " have been added to " + username + "'s account");
                    }));
                });
            }

            if(args[0].equalsIgnoreCase("set")) {
                if(sender instanceof Player) {
                    Player player = (Player)sender;

                    if(!player.hasPermission(Permissions.CORE_ADMIN)) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use this command");
                        return false;
                    }
                }

                String namedPlayer = args[1];
                String namedAmount = args[2];

                if(!NumberUtils.isNumber(namedAmount)) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount");
                    return false;
                }

                int setLives = NumberUtils.toInt(namedAmount);

                core.getOfflinePlayerLookup().getOfflinePlayerByName(namedPlayer, (uuid, username) -> {
                    if(uuid == null || username == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return;
                    }

                    core.getLives().setLives(uuid, setLives, (responseUUID, newLives) -> {
                        if(Bukkit.getPlayer(responseUUID) != null)
                            Bukkit.getPlayer(responseUUID).sendMessage(ChatColor.GREEN + "Your lives have been updated to " + newLives);

                        sender.sendMessage(ChatColor.GREEN + "You have updated " + username + "'s lives to " + newLives);

                        core.getLog().log(sender.getName() + " has updated " + username + "'s lives to " + newLives);
                    });
                });
            }

            return false;
        }

        return false;
    }
}
