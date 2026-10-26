/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with other players
 * Copyright (c) 2023 the-sugar-tree and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.utils.I18NUtil;
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
import java.util.function.Consumer;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class Commands implements TabExecutor {

    /** Permission node for {@code /inventoryshare}, declared in {@code plugin.yml} with {@code default: op}. */
    private static final String PERMISSION = "inventoryshare.command";

    private final PluginSettings settings;
    private final IInventoryManager inventoryManager;

    public Commands(PluginSettings settings, IInventoryManager inventoryManager) {
        this.settings = settings;
        this.inventoryManager = inventoryManager;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (!sender.hasPermission(PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is a mistake.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(usageMessage);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "inventory":
                    setting(sender, args, "inv", settings.isInventory(), settings::setInventory, true);
                    break;
                case "advancement":
                    setting(sender, args, "adv", settings.isAdvancement(), settings::setAdvancement, false);
                    break;
                case "announcedeath":
                    setting(sender, args, "andeath", settings.isAnnouncedeath(), settings::setAnnouncedeath, false);
                    break;
                case "teaminventory":
                    setting(sender, args, "teaminv", settings.isTeaminventory(), settings::setTeaminventory, true);
                    break;
                case "reload":
                    if (args.length != 1) {
                        sender.sendMessage(usageMessage);
                        break;
                    }
                    settings.reload();
                    // inventory / teaminventory may have changed, as with their commands
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        inventoryManager.updateInventory(p);
                    }
                    I18NUtil.reload();
                    updateUsageMessage();
                    Command.broadcastCommandMessage(sender, I18NUtil.get(true, "config_reloaded"));
                    break;
                case "check":
                    sender.sendMessage(checkMessage());
                    break;
                default:
                    sender.sendMessage(usageMessage);
            }
            return true;
        }
        return false;
    }

    /**
     * Shows a boolean setting, or sets it from {@code args[1]} ({@code true} / {@code false}).
     *
     * @param key       prefix of the setting's messages in the language file, e.g. {@code inv} for {@code inv_get},
     *                  {@code inv_set}, {@code inv_already_y} and {@code inv_already_n}
     * @param current   the setting's current value
     * @param set       stores a new value
     * @param reapply   whether online players' inventories must be applied again after a change
     */
    private void setting(CommandSender sender, String[] args, String key, boolean current, Consumer<Boolean> set,
                         boolean reapply) {
        if (args.length != 2) {
            sender.sendMessage(I18NUtil.get(true, key + "_get", String.valueOf(current)));
            return;
        }
        boolean value;
        switch (args[1].toLowerCase()) {
            case "true":
                value = true;
                break;
            case "false":
                value = false;
                break;
            default:
                sender.sendMessage(usageMessage);
                return;
        }
        if (value == current) {
            sender.sendMessage(I18NUtil.get(true, key + (value ? "_already_y" : "_already_n")));
            return;
        }
        set.accept(value);
        if (reapply) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                inventoryManager.updateInventory(p);
            }
        }
        Command.broadcastCommandMessage(sender, I18NUtil.get(true, key + "_set", String.valueOf(value)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (!sender.hasPermission(PERMISSION)) {
                return new ArrayList<>();
            }
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

    private String checkMessage() {
        return ChatColor.DARK_AQUA + "-----------------------------------------------------\n" +
                PREFIX + ChatColor.GOLD + "inventory: " + ChatColor.GREEN + settings.isInventory() + "\n" +
                PREFIX + ChatColor.GOLD + "advancement: " + ChatColor.GREEN + settings.isAdvancement() + "\n" +
                PREFIX + ChatColor.GOLD + "announcedeath: " + ChatColor.GREEN + settings.isAnnouncedeath() + "\n" +
                PREFIX + ChatColor.GOLD + "teaminventory: " + ChatColor.GREEN + settings.isTeaminventory() + "\n" +
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
}
