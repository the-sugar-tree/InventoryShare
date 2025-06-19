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

package com.sugar_tree.inventoryshare.v1_21_R4;

import com.sugar_tree.inventoryshare.PlayerInventory;
import com.sugar_tree.inventoryshare.api.IFileManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public class FileManager implements IFileManager {
    protected static Map<UUID, PlayerInventory> origianlPlayerInventoryMap = new HashMap<>();

    protected static PlayerInventory sharedInventory = new PlayerInventory(NonNullList.a(36, ItemStack.l));

    protected static Map<String, PlayerInventory> teamInventories = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    public void save() {
        List<Map<?, ?>> itemslist = new ArrayList<>();
        for (ItemStack itemStack : sharedInventory.getItems()) {
            itemslist.add(CraftItemStack.asCraftMirror(itemStack).serialize());
        }
        invconfig.set("items", itemslist);


        for (EnumItemSlot enumItemSlot : sharedInventory.getEquipment().keySet()) {
            List<Map<?, ?>> equipmentElement = new ArrayList<>();
            equipmentElement.add(CraftItemStack.asCraftMirror(sharedInventory.getEquipment().get(enumItemSlot)).serialize());
            invconfig.set(enumItemSlot.e(), equipmentElement);
        }


        List<String> alist = new ArrayList<>();
        for (NamespacedKey namespacedKey : advlist) {
            alist.add(namespacedKey.getKey());
        }
        advconfig.set("advancement", alist);

        plugin.getConfig().set("inventory", inventory);
        plugin.getConfig().set("advancement", advancement);
        plugin.getConfig().set("announcedeath", announcedeath);
        plugin.getConfig().set("teaminventory", teaminventory);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team == null) continue;
            File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> itemslistT = new ArrayList<>();
            PlayerInventory invT = teamInventories.get(team.getName());
            if (invT == null) continue;
            for (ItemStack itemStack : invT.getItems()) {
                itemslistT.add(CraftItemStack.asCraftMirror(itemStack).serialize());
            }
            fileConfiguration.set("items", itemslistT);

            // TODO: equipment
        }
        saveConfigs();
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "ConstantConditions"})
    public void load() {
        List<Map<?, ?>> itemslist = invconfig.getMapList("items");
        for (int i = 0; i <= itemslist.size(); i++) {
            try { itemslist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (itemslist.get(i).isEmpty()) {
                continue;
            }
            if (itemslist.get(i).containsKey("v") && Integer.parseInt(itemslist.get(i).get("v").toString()) > WORLD_VERSION) {
                logger.severe("Newer version! Server downgrades are not supported!");
                return;
            }
        }

        for (EnumItemSlot value : EnumItemSlot.values()) {
            List<Map<?, ?>> maps = invconfig.getMapList(value.e());
            if (maps == null || maps.isEmpty()) continue;
            for (Map<?, ?> map : maps) {
                if (map.isEmpty()) continue;
                sharedInventory.getEquipment().put(value, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) map)));
            }
        }


        List<String> alist = advconfig.getStringList("advancement");
        for (int i = 0; i <= alist.size(); i++) {
            try { alist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            advlist.add(plugin.getServer().getAdvancement(NamespacedKey.fromString(alist.get(i))).getKey());
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

        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            NonNullList<ItemStack> items = NonNullList.a(36, ItemStack.l);
            File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
            if (file.exists()) {
                FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
                List<Map<?, ?>> itemslistT = fileConfiguration.getMapList("items");
                for (int i = 0; i <= itemslistT.size(); i++) {
                    try { itemslistT.get(i); } catch (IndexOutOfBoundsException e) { break; }
                    if (itemslistT.get(i).isEmpty()) {
                        continue;
                    }
                    items.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) itemslistT.get(i))));
                }

                // TODO: equipment
            }
            teamInventories.put(team.getName(), new PlayerInventory(items));
            // TODO: equipment
        }
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public void deleteWasteFiles() {
        if (new File(plugin.getDataFolder(), "\\teams").listFiles() != null) {
            for (File file : (new File(plugin.getDataFolder(), "\\teams").listFiles())) {
                List<String> list = new ArrayList<>();
                for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                    list.add(team.getName());
                }
                if (!list.contains(file.getName())) {
                    file.delete();
                }
            }
        }
    }
}