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
import com.sugar_tree.inventoryshare.api.Inventory;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.sugar_tree.inventoryshare.api.Variables.plugin;
import static com.sugar_tree.inventoryshare.api.Variables.teaminventory;
import static com.sugar_tree.inventoryshare.v1_18_R1.FileManager_1_18_R1.*;

public class Inventory_1_18_R1 implements Inventory {

    public void invApplyAll(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.fq();
        try {
            setField(playerInventory, "h", items);
            setField(playerInventory, "i", armor);
            setField(playerInventory, "j", extraSlots);
            setField(playerInventory, "n", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void invDisApply(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.fq();
        if (invList.containsKey(entityPlayer.cm())) {
            try {
                NonNullList<ItemStack> items1 = invList.get(entityPlayer.cm()).h;
                NonNullList<ItemStack> armor1 = invList.get(entityPlayer.cm()).i;
                NonNullList<ItemStack> extraSlots1 = invList.get(entityPlayer.cm()).j;
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "h", items1);
                setField(playerInventory, "i", armor1);
                setField(playerInventory, "j", extraSlots1);
                setField(playerInventory, "n", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 사용될 일이 없지만, 혹시 모른 버그 방지
            try {
                NonNullList<ItemStack> items1 = NonNullList.a(36, ItemStack.b);
                NonNullList<ItemStack> armor1 = NonNullList.a(4, ItemStack.b);
                NonNullList<ItemStack> extraSlots1 = NonNullList.a(1, ItemStack.b);
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "h", items1);
                setField(playerInventory, "i", armor1);
                setField(playerInventory, "j", extraSlots1);
                setField(playerInventory, "n", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        invList.remove(entityPlayer);
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
        if (!InventoryList.containsKey(teamName)) {
            Map<String, NonNullList<ItemStack>> map = new HashMap<>();
            itemsT = NonNullList.a(36, ItemStack.b);
            armorT = NonNullList.a(4, ItemStack.b);
            extraSlotsT = NonNullList.a(1, ItemStack.b);
            map.put("items", itemsT);
            map.put("armor", armorT);
            map.put("extraSlots", extraSlotsT);
            InventoryList.put(teamName, map);
        } else {
            Map<String, NonNullList<ItemStack>> map = InventoryList.get(teamName);
            itemsT = map.get("items");
            armorT = map.get("armor");
            extraSlotsT = map.get("extraSlots");
        }
        List<NonNullList<ItemStack>> contentsT = ImmutableList.of(itemsT, armorT, extraSlotsT);
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.fq();
        try {
            setField(playerInventory, "h", itemsT);
            setField(playerInventory, "i", armorT);
            setField(playerInventory, "j", extraSlotsT);
            setField(playerInventory, "n", contentsT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerInventory(@NotNull Player p) {
        PlayerInventory pinv = new PlayerInventory(null);
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        try {
            setField(pinv, "h", entityPlayer.fq().h);
            setField(pinv, "i", entityPlayer.fq().i);
            setField(pinv, "j", entityPlayer.fq().j);
            setField(pinv, "n", ImmutableList.of(entityPlayer.fq().h, entityPlayer.fq().i, entityPlayer.fq().j));
        } catch (Exception e) {
            e.printStackTrace();
        }
        invList.put(p.getUniqueId(), pinv);
    }

    public Set<UUID> getRegisteredPlayers() {
        return invList.keySet();
    }
}