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

import com.sugar_tree.inventoryshare.util.I18NUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public class Commands implements TabExecutor {

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is a mistake.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(usageMessage);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "inventory":
                    if (args.length != 2) {
                        sender.sendMessage(I18NUtil.get("inv_get", true, String.valueOf(inventory)));
                        break;
                    }
                    switch (args[1].toLowerCase()) {
                        case "true":
                            if (!inventory) {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    InventoryManager.savePlayerInventory(p);
                                }
                                inventory = true;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("inv_set", true, String.valueOf(inventory)));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    InventoryManager.applyInventory(p);
                                    p.updateInventory();
                                }
                            } else {
                                sender.sendMessage(I18NUtil.get("inv_already_y", true));
                            }
                            break;
                        case "false":
                            if (inventory) {
                                inventory = false;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("inv_set", true, String.valueOf(inventory)));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    InventoryManager.disApplyInventory(p);
                                    p.updateInventory();
                                }
                            } else {
                                sender.sendMessage(I18NUtil.get("inv_already_n", true));
                            }
                            break;
                        default:
                            sender.sendMessage(usageMessage);
                    }
                    break;
                case "advancement":
                    if (args.length != 2) {
                        sender.sendMessage(I18NUtil.get("adv_get", true, String.valueOf(advancement)));
                        break;
                    }
                    switch (args[1].toLowerCase()) {
                        case "true":
                            if (!advancement) {
                                advancement = true;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("adv_set", true, String.valueOf(advancement)));
                            } else {
                                sender.sendMessage(I18NUtil.get("adv_already_y", true));
                            }
                            break;
                        case "false":
                            if (advancement) {
                                advancement = false;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("adv_set", true, String.valueOf(advancement)));
                            } else {
                                sender.sendMessage(I18NUtil.get("adv_already_n", true));
                            }
                            break;
                        default:
                            sender.sendMessage(usageMessage);
                    }
                    break;
                case "announcedeath":
                    if (args.length != 2) {
                        sender.sendMessage(I18NUtil.get("andeath_get", true, String.valueOf(teaminventory)));
                        break;
                    }
                    switch (args[1].toLowerCase()) {
                        case "true":
                            if (!announcedeath) {
                                announcedeath = true;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("andeath_set", true, String.valueOf(announcedeath)));
                            } else {
                                I18NUtil.get("andeath_already_y", true);
                            }
                            break;
                        case "false":
                            if (announcedeath) {
                                announcedeath = false;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("andeath_set", true, String.valueOf(announcedeath)));
                            } else {
                                I18NUtil.get("andeath_already_n", true);
                            }
                            break;
                        default:
                            sender.sendMessage(usageMessage);
                    }
                    break;
                case "teaminventory":
                    if (args.length != 2) {
                        sender.sendMessage(I18NUtil.get("teaminv_get", true, String.valueOf(teaminventory)));
                        break;
                    }
                    switch (args[1].toLowerCase()) {
                        case "true":
                            if (!teaminventory) {
                                teaminventory = true;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("teaminv_set", true, String.valueOf(teaminventory)));
                                if (inventory) {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        InventoryManager.applyInventory(p);
                                        p.updateInventory();
                                    }
                                }
                            } else {
                                sender.sendMessage(I18NUtil.get("teaminv_already_y", true));
                            }
                            break;
                        case "false":
                            if (teaminventory) {
                                teaminventory = false;
                                Command.broadcastCommandMessage(sender, I18NUtil.get("teaminv_set", true, String.valueOf(teaminventory)));
                                if (inventory) {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        InventoryManager.applyInventory(p);
                                        p.updateInventory();
                                    }
                                }
                            } else {
                                sender.sendMessage(I18NUtil.get("teaminv_already_n", true));
                            }
                            break;
                        default:
                            sender.sendMessage(usageMessage);
                    }
                    break;
                case "reload":
                    if (args.length != 1) {
                        sender.sendMessage(usageMessage);
                        break;
                    }
                    plugin.reloadConfig();
                    if (plugin.getConfig().contains("inventory")) advancement = plugin.getConfig().getBoolean("inventory");
                    if (plugin.getConfig().contains("advancement")) advancement = plugin.getConfig().getBoolean("advancement");
                    if (plugin.getConfig().contains("announcedeath")) announcedeath = plugin.getConfig().getBoolean("announcedeath");
                    if (plugin.getConfig().contains("teaminventory")) teaminventory = plugin.getConfig().getBoolean("teaminventory");
                    updateUsageMessage();
                    updateCheckMessage();
                    I18NUtil.reload();
                    sender.sendMessage(I18NUtil.get("config_reloaded", true));
                    break;
                case "check":
                    sender.sendMessage(check);
                    break;
                default:
                    sender.sendMessage(usageMessage);
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
                arrayList.add("announcedeath");
                arrayList.add("teaminventory");
                arrayList.add("check");
                arrayList.add("reload");
                arrayList.removeIf(s -> !s.startsWith(args[0]));
                return arrayList;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("inventory") || args[0].equalsIgnoreCase("advancement")
                        || args[0].equalsIgnoreCase("announcedeath") || args[0].equalsIgnoreCase("teaminventory")) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    arrayList.add("true");
                    arrayList.add("false");
                    arrayList.removeIf(s -> !s.startsWith(args[1]));
                    return arrayList;
                } else {
                    return new ArrayList<>();
                }
            } else {
                return new ArrayList<>();
            }
        }
        return null;
    }

    private void updateUsageMessage() {
        usageMessage = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
                I18NUtil.get("help_message1") + "\n" +
                I18NUtil.get("help_message2") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " inventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_inv_info") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " advancement " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_adv_info") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " announcedeath " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_andeath_info") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " teaminventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_teaminv_info") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " check" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_check_info") + "\n" +
                ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " reload" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_reload_config_info") + "\n" +
                ChatColor.DARK_AQUA + "-----------------------------------------------------"
                ;
    }

    private void updateCheckMessage() {
        check = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
                PREFIX + ChatColor.GOLD + "inventory: " + ChatColor.GREEN + inventory + "\n" +
                PREFIX + ChatColor.GOLD + "advancement: " + ChatColor.GREEN + advancement + "\n" +
                PREFIX + ChatColor.GOLD + "announcedeath: " + ChatColor.GREEN + announcedeath + "\n" +
                PREFIX + ChatColor.GOLD + "teaminventory: " + ChatColor.GREEN + teaminventory + "\n" +
                ChatColor.DARK_AQUA + "-----------------------------------------------------"
                ;
    }

    String usageMessage = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
            I18NUtil.get("help_message1") + "\n" +
            I18NUtil.get("help_message2") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " inventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_inv_info") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " advancement " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_adv_info") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " announcedeath " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_andeath_info") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " teaminventory " + ChatColor.GOLD + "[true|false]" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_teaminv_info") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " check" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_check_info") + "\n" +
            ChatColor.AQUA + "/inventoryshare" + ChatColor.GREEN + " reload" + ChatColor.YELLOW + " - " + I18NUtil.get("cmd_reload_config_info") + "\n" +
            ChatColor.DARK_AQUA + "-----------------------------------------------------"
            ;
    String check = ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
            PREFIX + ChatColor.GOLD + "inventory: " + ChatColor.GREEN + inventory + "\n" +
            PREFIX + ChatColor.GOLD + "advancement: " + ChatColor.GREEN + advancement + "\n" +
            PREFIX + ChatColor.GOLD + "announcedeath: " + ChatColor.GREEN + announcedeath + "\n" +
            PREFIX + ChatColor.GOLD + "teaminventory: " + ChatColor.GREEN + teaminventory + "\n" +
            ChatColor.DARK_AQUA + "-----------------------------------------------------"
            ;
}
