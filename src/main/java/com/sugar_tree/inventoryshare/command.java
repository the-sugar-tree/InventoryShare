package com.sugar_tree.inventoryshare;

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

import static com.sugar_tree.inventoryshare.Inventory.load;
import static com.sugar_tree.inventoryshare.InventoryShare.*;

public class command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventoryshare")) {
            if (sender.isOp()) {
                if (args.length >= 1) {
                    /*
                    if (args[0].equalsIgnoreCase("inventory")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                inventory = true;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "인벤토리 공유: " + ChatColor.GREEN + inventory + ChatColor.YELLOW + "로 설정되었습니다.");
                            }
                            else if (args[1].equalsIgnoreCase("false")) {
                                if (inventory) {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        p.getInventory().clear();
                                    }
                                }
                                inventory = false;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "인벤토리 공유: " + ChatColor.GREEN + inventory + ChatColor.YELLOW + "로 설정되었습니다.");
                            }
                            else {
                                sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요!" + command.getUsage());
                            }
                        }
                        else {
                            sender.sendMessage(PREFIX + ChatColor.YELLOW + "인벤토리 공유: " + ChatColor.GREEN + inventory);
                        }
                    }
                    else*/ if (args[0].equalsIgnoreCase("advancement")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                advancement = true;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "발전과제 공유: " + ChatColor.GREEN + advancement + ChatColor.YELLOW + "로 설정되었습니다.");

                            }
                            else if (args[1].equalsIgnoreCase("false")) {
                                advancement = false;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "발전과제 공유: " + ChatColor.GREEN + advancement + ChatColor.YELLOW + "로 설정되었습니다.");
                            }
                            else {
                                sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요!" + command.getUsage());
                            }
                        }
                        else {
                            sender.sendMessage(PREFIX + ChatColor.YELLOW + "발전과제 공유: " + ChatColor.GREEN + advancement);
                        }
                    }
                    else if (args[0].equalsIgnoreCase("AnnounceDeath")) {
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("true")) {
                                AnnounceDeath = true;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "사망 시 좌표출력: " + ChatColor.GREEN + AnnounceDeath + ChatColor.YELLOW + "로 설정되었습니다.");
                            }
                            else if (args[1].equalsIgnoreCase("false")) {
                                AnnounceDeath = false;
                                sender.sendMessage(PREFIX + ChatColor.YELLOW + "사망 시 좌표출력: " + ChatColor.GREEN + AnnounceDeath + ChatColor.YELLOW + "로 설정되었습니다.");
                            }
                            else {
                                sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요!" + command.getUsage());
                            }
                        }
                        else {
                            sender.sendMessage(PREFIX + ChatColor.YELLOW + "사망 시 좌표출력: " + ChatColor.GREEN + AnnounceDeath);
                        }
                    }
                    else if (args[0].equalsIgnoreCase("reload")) {
                        if (args.length == 1) {
                            load();
                            sender.sendMessage(PREFIX + ChatColor.GREEN + "Config 파일이 새로고침 되었습니다!");
                        }
                        else {
                            sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요!" + command.getUsage());
                        }
                    }
                    else {
                        sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요! " + command.getUsage());
                    }
                }
                else {
                    sender.sendMessage(PREFIX + ChatColor.RED + "제대로 입력해주세요! " + command.getUsage());
                }
            }
            else {
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
//                arrayList.add("inventory");
                arrayList.add("advancement");
                arrayList.add("AnnounceDeath");
                arrayList.add("reload");
                return arrayList;
            }
            else if (args.length == 2) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("true");
                arrayList.add("false");
                return arrayList;
            }
            else {
                return new ArrayList<>();
            }
        }
        return null;
    }
}
