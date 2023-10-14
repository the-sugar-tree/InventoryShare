/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with ohter players
 * Copyright (c) 2023 the-sugar-tree
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

package com.sugar_tree.inventoryshare.nms;

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.sugar_tree.inventoryshare.SharedConstants.*;
import static com.sugar_tree.inventoryshare.nms.NMSLoader.*;

public final class FileManager implements IFileManager {

    public FileManager() {}

    @Override
    public void save() {
        List<Map<?, ?>> serializedItemsList = new ArrayList<>();
        for (Object itemStack : sharedItems) {
            serializedItemsList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
        }
        invconfig.set("items", serializedItemsList);

        List<Map<?, ?>> serializedArmorList = new ArrayList<>();
        for (Object itemStack : sharedArmor) {
            serializedArmorList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
        }
        invconfig.set("armor", serializedArmorList);

        List<Map<?, ?>> serializedExtraSlotsList = new ArrayList<>();
        for (Object itemStack : sharedExtraSlots) {
            serializedExtraSlotsList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
        }
        invconfig.set("extraSlots", serializedExtraSlotsList);

        List<String> serializedAdvancementList = new ArrayList<>();
        for (NamespacedKey namespacedKey : advlist) {
            serializedAdvancementList.add(namespacedKey.getKey());
        }
        advconfig.set("advancement", serializedAdvancementList);

        plugin.getConfig().set("inventory", inventory);
        plugin.getConfig().set("advancement", advancement);
        plugin.getConfig().set("announcedeath", announcedeath);
        plugin.getConfig().set("teaminventory", teaminventory);
        StringBuilder sb = new StringBuilder();
        boolean savedLeastOneTeam = false;
        sb.append(I18N_TEAM_SAVED);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team == null) continue;
            File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> serializedTeamItemsList = new ArrayList<>();
            PlayerInventory teamInventory = TeamInventoryMap.get(team.getName());
            if (teamInventory == null) continue;
            for (Object itemStack : teamInventory.getItems()) {
                serializedTeamItemsList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
            }
            fileConfiguration.set("items", serializedTeamItemsList);

            List<Map<?, ?>> serializedTeamArmorList = new ArrayList<>();
            for (Object itemStack : teamInventory.getArmor()) {
                serializedTeamArmorList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
            }
            fileConfiguration.set("armor", serializedTeamArmorList);

            List<Map<?, ?>> serializedTeamExtraSlotsList = new ArrayList<>();
            for (Object itemStack : teamInventory.getExtraSlots()) {
                serializedTeamExtraSlotsList.add(NMSLoader.asBukkitCopy(itemStack).serialize());
            }
            fileConfiguration.set("extraSlots", serializedTeamExtraSlotsList);
            teamInvFileList.put(fileConfiguration, file);
            sb.append("[").append(team.getName()).append("] ");
            savedLeastOneTeam = true;
        }
        if (savedLeastOneTeam) logger.info(sb.toString());
        saveConfigs();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load() {
        List<Map<?, ?>> itemslist = invconfig.getMapList("items");
        for (int i = 0; i <= itemslist.size(); i++) {
            try {
                if (itemslist.get(i).isEmpty()) {
                    continue;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (itemslist.get(i).containsKey("v") && Integer.parseInt(itemslist.get(i).get("v").toString()) > WORLD_VERSION) {
                logger.severe("Newer version! Server downgrades are not supported!");
                return;
            }
            sharedItems.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) itemslist.get(i))));
        }

        List<Map<?, ?>> armorlist = invconfig.getMapList("armor");
        for (int i = 0; i <= armorlist.size(); i++) {
            try {
                if (armorlist.get(i).isEmpty()) {
                    continue;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            sharedArmor.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) armorlist.get(i))));
        }

        List<Map<?, ?>> extraSlotslist = invconfig.getMapList("extraSlots");
        for (int i = 0; i <= extraSlotslist.size(); i++) {
            try {
                if (extraSlotslist.get(i).isEmpty()) {
                    continue;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            sharedExtraSlots.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) extraSlotslist.get(i))));
        }

        List<String> alist = advconfig.getStringList("advancement");
        for (int i = 0; i <= alist.size(); i++) {
            try {
                advlist.add(plugin.getServer().getAdvancement(NMSLoader.getNamespacedKey(alist.get(i))).getKey());
            } catch (NullPointerException ignored) {
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }

        if (plugin.getConfig().contains("inventory")) {
            inventory = plugin.getConfig().getBoolean("inventory");
        }
        if (plugin.getConfig().contains("advancement")) {
            advancement = plugin.getConfig().getBoolean("advancement");
        }
        if (plugin.getConfig().contains("announcedeath")) {
            announcedeath = plugin.getConfig().getBoolean("announcedeath");
        }
        if (plugin.getConfig().contains("teaminventory")) {
            teaminventory = plugin.getConfig().getBoolean("teaminventory");
        }

        StringBuilder sb = new StringBuilder();
        boolean temp = false;
        sb.append(I18N_TEAM_LOADED);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            AbstractList<Object> items = NMSLoader.createEmptyItemList(36);
            AbstractList<Object> armor = NMSLoader.createEmptyItemList(4);
            AbstractList<Object> extraSlots = NMSLoader.createEmptyItemList(1);
            File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
            if (file.exists()) {
                FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
                List<Map<?, ?>> itemslistT = fileConfiguration.getMapList("items");
                for (int i = 0; i <= itemslistT.size(); i++) {
                    try {
                        if (itemslistT.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    items.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) itemslistT.get(i))));
                }

                List<Map<?, ?>> armorlistT = fileConfiguration.getMapList("armor");
                for (int i = 0; i <= armorlistT.size(); i++) {
                    try {
                        if (armorlistT.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    armor.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) armorlistT.get(i))));
                }

                List<Map<?, ?>> extraSlotslistT = fileConfiguration.getMapList("extraSlots");
                for (int i = 0; i <= extraSlotslistT.size(); i++) {
                    try {
                        if (extraSlotslistT.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    extraSlots.set(i, NMSLoader.asNMSCopy(ItemStack.deserialize((Map<String, Object>) extraSlotslistT.get(i))));
                }
            }
            TeamInventoryMap.put(team.getName(), new PlayerInventory(items, armor, extraSlots));
            sb.append("[").append(team.getName()).append("] ");
            temp = true;
        }
        if (temp) logger.info(sb.toString());
    }

    @Override
    public void deleteWasteFiles() {
        File[] files = new File(plugin.getDataFolder(), "\\teams").listFiles();
        if (files != null) {
            for (File file : files) {
                List<String> list = new ArrayList<>();
                for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                    list.add(team.getName());
                }
                if (!list.contains(file.getName())) {
                    try {
                        Files.delete(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}