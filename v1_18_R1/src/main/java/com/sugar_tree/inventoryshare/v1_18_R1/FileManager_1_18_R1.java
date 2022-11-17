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
package com.sugar_tree.inventoryshare.v1_18_R1;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.FileManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public class FileManager_1_18_R1 implements FileManager {
    protected static Map<UUID, PlayerInventory> invList = new HashMap<>();

    protected static NonNullList<ItemStack> items = NonNullList.a(36, ItemStack.b);
    protected static NonNullList<ItemStack> armor = NonNullList.a(4, ItemStack.b);
    protected static NonNullList<ItemStack> extraSlots = NonNullList.a(1, ItemStack.b);
    protected static List<NonNullList<ItemStack>> contents = ImmutableList.of(items, armor, extraSlots);
    protected static Map<String, Map<String, NonNullList<ItemStack>>> InventoryList = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    public void save() {
        List<Map<?, ?>> itemslist = new ArrayList<>();
        for (ItemStack itemStack : items) {
            itemslist.add(CraftItemStack.asCraftMirror(itemStack).serialize());
        }
        invconfig.set("items", itemslist);

        List<Map<?, ?>> armorlist = new ArrayList<>();
        for (ItemStack itemStack : armor) {
            armorlist.add(CraftItemStack.asCraftMirror(itemStack).serialize());
        }
        invconfig.set("armor", armorlist);

        List<Map<?, ?>> extraSlotsList = new ArrayList<>();
        for (ItemStack itemStack : extraSlots) {
            extraSlotsList.add(CraftItemStack.asCraftMirror(itemStack).serialize());
        }
        invconfig.set("extraSlots", extraSlotsList);

        List<String> alist = new ArrayList<>();
        for (NamespacedKey namespacedKey : advlist) {
            alist.add(namespacedKey.getKey());
        }
        advconfig.set("advancement", alist);

        plugin.getConfig().set("inventory", inventory);
        plugin.getConfig().set("advancement", advancement);
        plugin.getConfig().set("AnnounceDeath", AnnounceDeath);
        plugin.getConfig().set("teaminventory", teaminventory);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team == null) continue;
            File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> itemslistT = new ArrayList<>();
            Map<String, NonNullList<ItemStack>> invT = InventoryList.get(team.getName());
            if (invT == null) continue;
            for (ItemStack itemStack : invT.get("items")) {
                itemslistT.add(CraftItemStack.asCraftMirror(itemStack).serialize());
            }
            fileConfiguration.set("items", itemslistT);

            List<Map<?, ?>> armorlistT = new ArrayList<>();
            for (ItemStack itemStack : invT.get("armor")) {
                armorlistT.add(CraftItemStack.asCraftMirror(itemStack).serialize());
            }
            fileConfiguration.set("armor", armorlistT);

            List<Map<?, ?>> extraSlotsListT = new ArrayList<>();
            for (ItemStack itemStack : invT.get("extraSlots")) {
                extraSlotsListT.add(CraftItemStack.asCraftMirror(itemStack).serialize());
            }
            fileConfiguration.set("extraSlots", extraSlotsListT);
            teamInvFileList.put(fileConfiguration, file);
        }
        saveConfigs(plugin);
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "ConstantConditions"})
    public void load() {
        List<Map<?, ?>> itemslist = invconfig.getMapList("items");
        for (int i = 0; i <= itemslist.size(); i++) {
            try { itemslist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (itemslist.get(i).isEmpty()) {
                continue;
            }
            if (Integer.parseInt(((Map<String, Object>) itemslist.get(i)).get("v").toString()) > WorldVersion) {
                logger.severe("Newer version! Server downgrades are not supported!");
                return;
            }
            items.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) itemslist.get(i))));
        }

        List<Map<?, ?>> armorlist = invconfig.getMapList("armor");
        for (int i = 0; i <= armorlist.size(); i++) {
            try { armorlist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (armorlist.get(i).isEmpty()) {
                continue;
            }
            armor.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) armorlist.get(i))));
        }

        List<Map<?, ?>> extraSlotslist = invconfig.getMapList("extraSlots");
        for (int i = 0; i <= extraSlotslist.size(); i++) {
            try { extraSlotslist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (extraSlotslist.get(i).isEmpty()) {
                continue;
            }
            extraSlots.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) extraSlotslist.get(i))));
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
        if (plugin.getConfig().contains("AnnounceDeath")) {
            AnnounceDeath = plugin.getConfig().getBoolean("AnnounceDeath");
        }
        if (plugin.getConfig().contains("teaminventory")) {
            teaminventory = plugin.getConfig().getBoolean("teaminventory");
        }

        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            NonNullList<ItemStack> items = NonNullList.a(36, ItemStack.b);
            NonNullList<ItemStack> armor = NonNullList.a(4, ItemStack.b);
            NonNullList<ItemStack> extraSlots = NonNullList.a(1, ItemStack.b);
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

                List<Map<?, ?>> armorlistT = fileConfiguration.getMapList("armor");
                for (int i = 0; i <= armorlistT.size(); i++) {
                    try { armorlistT.get(i); } catch (IndexOutOfBoundsException e) { break; }
                    if (armorlistT.get(i).isEmpty()) {
                        continue;
                    }
                    armor.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) armorlistT.get(i))));
                }

                List<Map<?, ?>> extraSlotslistT = fileConfiguration.getMapList("extraSlots");
                for (int i = 0; i <= extraSlotslistT.size(); i++) {
                    try { extraSlotslistT.get(i); } catch (IndexOutOfBoundsException e) { break; }
                    if (extraSlotslistT.get(i).isEmpty()) {
                        continue;
                    }
                    extraSlots.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) extraSlotslistT.get(i))));
                }
            }
            Map<String, NonNullList<ItemStack>> m = new HashMap<>();
            m.put("items", items);
            m.put("armor", armor);
            m.put("extraSlots", extraSlots);
            InventoryList.put(team.getName(), m);
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
