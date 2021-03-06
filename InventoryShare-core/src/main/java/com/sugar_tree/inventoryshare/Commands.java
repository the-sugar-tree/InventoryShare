/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sugar_tree.inventoryshare;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.sugar_tree.inventoryshare.InventoryShare.*;
import static com.sugar_tree.inventoryshare.api.Variables.*;

public class Commands implements TabExecutor {
    private static final Plugin plugin = getPlugin(InventoryShare.class);

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (sender.isOp()) {
                if (args.length >= 1) {
                    if (args[0].equalsIgnoreCase("inventory")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                if (!inventory) {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        InventoryClass.savePlayerInventory(p);
                                    }
                                    inventory = true;
                                    Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + inventory + ChatColor.GOLD + "??? ?????????????????????");
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        InventoryClass.invApply(p);
                                        p.updateInventory();
                                    }
                                } else {
                                    sender.sendMessage(PREFIX + ChatColor.RED + "?????? ???????????? ????????? ????????? ?????? ????????????!");
                                }
                            } else if (args[1].equalsIgnoreCase("false")) {
                                if (inventory) {
                                    inventory = false;
                                    Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + inventory + ChatColor.GOLD + "??? ?????????????????????");
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        InventoryClass.invDisApply(p);
                                        p.updateInventory();
                                    }
                                } else {
                                    sender.sendMessage(PREFIX + ChatColor.RED + "?????? ???????????? ????????? ???????????? ?????? ????????????!");
                                }
                            } else {
                                sender.sendMessage(usageMessage);
                            }
                        } else {
                            sender.sendMessage(PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + inventory);
                        }
                    } else if (args[0].equalsIgnoreCase("advancement")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                advancement = true;
                                Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + advancement + ChatColor.GOLD + "??? ?????????????????????");
                            } else if (args[1].equalsIgnoreCase("false")) {
                                advancement = false;
                                Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + advancement + ChatColor.GOLD + "??? ?????????????????????");
                            } else {
                                sender.sendMessage(usageMessage);
                            }
                        } else {
                            sender.sendMessage(PREFIX + ChatColor.GOLD + "???????????? ??????: " + ChatColor.GREEN + advancement);
                        }
                    } else if (args[0].equalsIgnoreCase("AnnounceDeath")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                AnnounceDeath = true;
                                Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "?????? ??? ????????????: " + ChatColor.GREEN + AnnounceDeath + ChatColor.GOLD + "??? ?????????????????????");
                            } else if (args[1].equalsIgnoreCase("false")) {
                                AnnounceDeath = false;
                                Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "?????? ??? ????????????: " + ChatColor.GREEN + AnnounceDeath + ChatColor.GOLD + "??? ?????????????????????");
                            } else {
                                sender.sendMessage(usageMessage);
                            }
                        } else {
                            sender.sendMessage(PREFIX + ChatColor.GOLD + "?????? ??? ????????????: " + ChatColor.GREEN + AnnounceDeath);
                        }
                    } else if (args[0].equalsIgnoreCase("teaminventory")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                if (!teaminventory) {
                                    teaminventory = true;
                                    Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "??? ????????? ??????: " + ChatColor.GREEN + teaminventory + ChatColor.GOLD + "??? ?????????????????????");
                                    if (inventory) {
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            InventoryClass.invApply(p);
                                            p.updateInventory();
                                        }
                                    }
                                } else {
                                    sender.sendMessage(PREFIX + ChatColor.RED + "?????? ??? ????????? ????????? ????????? ?????? ????????????!");
                                }
                            } else if (args[1].equalsIgnoreCase("false")) {
                                if (teaminventory) {
                                    teaminventory = false;
                                    Command.broadcastCommandMessage(sender, PREFIX + ChatColor.GOLD + "??? ????????? ??????: " + ChatColor.GREEN + teaminventory + ChatColor.GOLD + "??? ?????????????????????");
                                    if (inventory) {
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            InventoryClass.invApply(p);
                                            p.updateInventory();
                                        }
                                    }
                                } else {
                                    sender.sendMessage(PREFIX + ChatColor.RED + "?????? ??? ????????? ????????? ???????????? ?????? ????????????!");
                                }
                            } else {
                                sender.sendMessage(usageMessage);
                            }
                        } else {
                            sender.sendMessage(PREFIX + ChatColor.GOLD + "??? ????????? ??????: " + ChatColor.GREEN + teaminventory);
                        }
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        if (args.length == 1) {
                            if (plugin.getConfig().contains("inventory")) advancement = plugin.getConfig().getBoolean("inventory");
                            if (plugin.getConfig().contains("advancement")) advancement = plugin.getConfig().getBoolean("advancement");
                            if (plugin.getConfig().contains("AnnounceDeath")) AnnounceDeath = plugin.getConfig().getBoolean("AnnounceDeath");
                            if (plugin.getConfig().contains("teaminventory")) AnnounceDeath = plugin.getConfig().getBoolean("teaminventory");
                            sender.sendMessage(PREFIX + ChatColor.GREEN + "Config ????????? ???????????? ???????????????!");
                        } else {
                            sender.sendMessage(usageMessage);
                        }
                    } else {
                        sender.sendMessage(usageMessage);
                    }
                } else {
                    sender.sendMessage(usageMessage);
                }
            } else {
                sender.sendMessage(PREFIX + Bukkit.getPermissionMessage());
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (args.length == 1) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("inventory");
                arrayList.add("advancement");
                arrayList.add("AnnounceDeath");
                arrayList.add("teaminventory");
                arrayList.add("reload");
                arrayList.removeIf(s -> !s.startsWith(args[0]));
                return arrayList;
            } else if (args.length == 2) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("true");
                arrayList.add("false");
                arrayList.removeIf(s -> !s.startsWith(args[1]));
                return arrayList;
            } else {
                return new ArrayList<>();
            }
        }
        return null;
    }

    String usageMessage = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
            PREFIX + ChatColor.GREEN + "????????? ?????????\n" +
            ChatColor.LIGHT_PURPLE + "(true??? false??? ???????????? ?????? ???????????? ???????????? ?????? ????????? ???????????????.)\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " inventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - ?????? ???????????? ?????? ????????? ???????????????.\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " advancement " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - ?????? ???????????? ?????? ????????? ???????????????.\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " AnnounceDeath " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - ?????? ????????? ?????? ?????? ????????? ???????????????.\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " teaminventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - ?????? ??? ????????? ?????? ????????? ???????????????.\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " reload" + ChatColor.YELLOW + " - config ????????? ???????????? ?????????.\n" +
            ChatColor.DARK_AQUA + "-----------------------------------------------------"
            ;
    String check = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
            PREFIX + ChatColor.GOLD + "inventory: " + ChatColor.GREEN + inventory +
            PREFIX + ChatColor.GOLD + "advancement: " + ChatColor.GREEN + advancement +
            PREFIX + ChatColor.GOLD + "AnnounceDeath: " + ChatColor.GREEN + AnnounceDeath +
            PREFIX + ChatColor.GOLD + "teaminventory: " + ChatColor.GREEN + teaminventory +
            ChatColor.DARK_AQUA + "-----------------------------------------------------"
            ;
}
