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
package com.sugar_tree.inventoryshare.v1_15_R1;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.Inventory;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.NonNullList;
import net.minecraft.server.v1_15_R1.PlayerInventory;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.sugar_tree.inventoryshare.api.Variables.plugin;
import static com.sugar_tree.inventoryshare.api.Variables.teaminventory;

public class Inventory_1_15_R1  implements Inventory {

    public void invApplyAll(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.inventory;
        try {
            setField(playerInventory, "items", FileManager_1_15_R1.items);
            setField(playerInventory, "armor", FileManager_1_15_R1.armor);
            setField(playerInventory, "extraSlots", FileManager_1_15_R1.extraSlots);
            setField(playerInventory, "f", FileManager_1_15_R1.contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void invDisApply(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.inventory;
        if (FileManager_1_15_R1.invList.containsKey(entityPlayer.getUniqueID())) {
            try {
                NonNullList<ItemStack> items1 = FileManager_1_15_R1.invList.get(entityPlayer.getUniqueID()).items;
                NonNullList<ItemStack> armor1 = FileManager_1_15_R1.invList.get(entityPlayer.getUniqueID()).armor;
                NonNullList<ItemStack> extraSlots1 = FileManager_1_15_R1.invList.get(entityPlayer.getUniqueID()).extraSlots;
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "items", items1);
                setField(playerInventory, "armor", armor1);
                setField(playerInventory, "extraSlots", extraSlots1);
                setField(playerInventory, "f", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 사용될 일이 없지만, 혹시 모른 버그 방지
            try {
                NonNullList<ItemStack> items1 = NonNullList.a(36, ItemStack.a);
                NonNullList<ItemStack> armor1 = NonNullList.a(4, ItemStack.a);
                NonNullList<ItemStack> extraSlots1 = NonNullList.a(1, ItemStack.a);
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "items", items1);
                setField(playerInventory, "armor", armor1);
                setField(playerInventory, "extraSlots", extraSlots1);
                setField(playerInventory, "f", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileManager_1_15_R1.invList.remove(entityPlayer);
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public void invApply(@NotNull Player p) {
        if (!(teaminventory)) {
            invApplyAll(p);
            return;
        }
        if (plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p) == null) {
            invApplyAll(p);
            return;
        }
        String teamName = plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p).getName();
        NonNullList<ItemStack> itemsT;
        NonNullList<ItemStack> armorT;
        NonNullList<ItemStack> extraSlotsT;
        if (!FileManager_1_15_R1.InventoryList.containsKey(teamName)) {
            Map<String, NonNullList<ItemStack>> map = new HashMap<>();
            itemsT = NonNullList.a(36, ItemStack.a);
            armorT = NonNullList.a(4, ItemStack.a);
            extraSlotsT = NonNullList.a(1, ItemStack.a);
            map.put("items", itemsT);
            map.put("armor", armorT);
            map.put("extraSlots", extraSlotsT);
            FileManager_1_15_R1.InventoryList.put(teamName, map);
        } else {
            Map<String, NonNullList<ItemStack>> map = FileManager_1_15_R1.InventoryList.get(teamName);
            itemsT = map.get("items");
            armorT = map.get("armor");
            extraSlotsT = map.get("extraSlots");
        }
        List<NonNullList<ItemStack>> contentsT = ImmutableList.of(itemsT, armorT, extraSlotsT);
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.inventory;
        try {
            setField(playerInventory, "items", itemsT);
            setField(playerInventory, "armor", armorT);
            setField(playerInventory, "extraSlots", extraSlotsT);
            setField(playerInventory, "f", contentsT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerInventory(@NotNull Player p) {
        PlayerInventory pinv = new PlayerInventory(null);
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        try {
            setField(pinv, "items", entityPlayer.inventory.items);
            setField(pinv, "armor", entityPlayer.inventory.armor);
            setField(pinv, "extraSlots", entityPlayer.inventory.extraSlots);
            setField(pinv, "f", ImmutableList.of(entityPlayer.inventory.items, entityPlayer.inventory.armor, entityPlayer.inventory.extraSlots));
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileManager_1_15_R1.invList.put(p.getUniqueId(), pinv);
    }

    public Set<UUID> getRegisteredPlayers() {
        return FileManager_1_15_R1.invList.keySet();
    }
}
