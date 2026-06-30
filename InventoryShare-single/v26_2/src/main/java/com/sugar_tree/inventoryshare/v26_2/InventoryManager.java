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

package com.sugar_tree.inventoryshare.v26_2;

import com.sugar_tree.inventoryshare.api.IInventoryManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Set;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;
import static com.sugar_tree.inventoryshare.api.SharedConstants.teaminventory;
import static com.sugar_tree.inventoryshare.v26_2.FileManager.*;

public class InventoryManager implements IInventoryManager {

    public void applyAllInventory(@NotNull Player p) {
        ServerPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        Inventory playerInventory = entityPlayer.getInventory();
        try {
            setField(playerInventory, "items", sharedInventory.getItems());
            setEquipment(playerInventory, sharedInventory.getEquipment());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private EnumMap<EquipmentSlot, ItemStack> getEquipmentInside(Inventory inventory) {
        try {
            Field field = EntityEquipment.class.getDeclaredField("items");
            field.setAccessible(true);
            return ((EnumMap<EquipmentSlot, ItemStack>) field.get(getEquipment(inventory)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private EntityEquipment getEquipment(Inventory inventory) throws NoSuchFieldException, IllegalAccessException {
        Field field = Inventory.class.getDeclaredField("equipment");
        field.setAccessible(true);
        return ((EntityEquipment) field.get(inventory));
    }

    private void setEquipment(Inventory inventory, EnumMap<EquipmentSlot, ItemStack> from) throws NoSuchFieldException, IllegalAccessException {
        Field field = EntityEquipment.class.getDeclaredField("items");
        field.setAccessible(true);
        field.set(getEquipment(inventory), from);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void disApplyInventory(@NotNull Player p) {
        ServerPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        Inventory playerInventory = entityPlayer.getInventory();
        if (origianlPlayerInventoryMap.containsKey(p.getUniqueId())) {
            try {
                NonNullList<ItemStack> items = origianlPlayerInventoryMap.get(p.getUniqueId()).getItems();
                EnumMap<EquipmentSlot, ItemStack> equipment = origianlPlayerInventoryMap.get(p.getUniqueId()).getEquipment();
                setField(playerInventory, "items", items);
                setEquipment(playerInventory, equipment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 사용될 일이 없지만, 혹시 모른 버그 방지
            throw new RuntimeException("이게 무슨 에러인지 저도 모르겠습니다.");
        }
        origianlPlayerInventoryMap.remove(entityPlayer);
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
        EnumMap<EquipmentSlot, ItemStack> equipmentT;
        if (!teamInventories.containsKey(teamName)) {
            itemsT = NonNullList.withSize(36, ItemStack.EMPTY);
            equipmentT = new EnumMap<>(EquipmentSlot.class);
            teamInventories.put(teamName, new com.sugar_tree.inventoryshare.PlayerInventory(itemsT, equipmentT));
        } else {
            com.sugar_tree.inventoryshare.PlayerInventory teamInventory = teamInventories.get(teamName);
            itemsT = teamInventory.getItems();
            equipmentT = teamInventory.getEquipment();
        }
        ServerPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        Inventory playerInventory = entityPlayer.getInventory();
        try {
            setField(playerInventory, "items", itemsT);
            setEquipment(playerInventory, equipmentT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerInventory(@NotNull Player p) {
        ServerPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        com.sugar_tree.inventoryshare.PlayerInventory pinv;
        pinv = new com.sugar_tree.inventoryshare.PlayerInventory(entityPlayer.getInventory().getNonEquipmentItems(), getEquipmentInside(entityPlayer.getInventory()));
        origianlPlayerInventoryMap.put(p.getUniqueId(), pinv);
    }

    public Set<UUID> getRegisteredPlayers() {
        return origianlPlayerInventoryMap.keySet();
    }
}