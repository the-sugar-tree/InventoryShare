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

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.IInventoryManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;
import static com.sugar_tree.inventoryshare.api.SharedConstants.teaminventory;
import static com.sugar_tree.inventoryshare.v1_21_R4.FileManager.*;

public class InventoryManager implements IInventoryManager {

    public void applyAllInventory(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.gj();
        try {
            setField(playerInventory, "g", items);
            setField(playerInventory, "h", armor);
            setField(playerInventory, "i", extraSlots);
            setField(playerInventory, "l", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void disApplyInventory(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.gj();
        if (invList.containsKey(p.getUniqueId())) {
            try {
                NonNullList<ItemStack> items1 = invList.get(p.getUniqueId()).g;
                NonNullList<ItemStack> armor1 = invList.get(p.getUniqueId()).h;
                NonNullList<ItemStack> extraSlots1 = invList.get(p.getUniqueId()).i;
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "g", items1);
                setField(playerInventory, "h", armor1);
                setField(playerInventory, "i", extraSlots1);
                setField(playerInventory, "l", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 사용될 일이 없지만, 혹시 모른 버그 방지
            try {
                NonNullList<ItemStack> items1 = NonNullList.a(36, ItemStack.l);
                NonNullList<ItemStack> armor1 = NonNullList.a(4, ItemStack.l);
                NonNullList<ItemStack> extraSlots1 = NonNullList.a(1, ItemStack.l);
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "g", items1);
                setField(playerInventory, "h", armor1);
                setField(playerInventory, "i", extraSlots1);
                setField(playerInventory, "l", contents1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        invList.remove(entityPlayer);
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public void applyInventory(@NotNull Player p) {
        if (!(teaminventory)) {
            applyAllInventory(p);
            return;
        }
        if (plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p) == null) {
            applyAllInventory(p);
            return;
        }
        String teamName = plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p).getName();
        NonNullList<ItemStack> itemsT;
        NonNullList<ItemStack> armorT;
        NonNullList<ItemStack> extraSlotsT;
        if (!InventoryList.containsKey(teamName)) {
            Map<String, NonNullList<ItemStack>> map = new HashMap<>();
            itemsT = NonNullList.a(36, ItemStack.l);
            armorT = NonNullList.a(4, ItemStack.l);
            extraSlotsT = NonNullList.a(1, ItemStack.l);
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
        PlayerInventory playerInventory = entityPlayer.gj();
        try {
            setField(playerInventory, "g", itemsT);
            setField(playerInventory, "h", armorT);
            setField(playerInventory, "i", extraSlotsT);
            setField(playerInventory, "l", contentsT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerInventory(@NotNull Player p) {
        PlayerInventory pinv = new PlayerInventory(null);
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        try {
            setField(pinv, "g", entityPlayer.gj().g);
            setField(pinv, "h", entityPlayer.gj().h);
            setField(pinv, "i", entityPlayer.gj().i);
            setField(pinv, "l", ImmutableList.of(entityPlayer.gj().g, entityPlayer.gj().h, entityPlayer.gj().i));
        } catch (Exception e) {
            e.printStackTrace();
        }
        invList.put(p.getUniqueId(), pinv);
    }

    public Set<UUID> getRegisteredPlayers() {
        return invList.keySet();
    }
}