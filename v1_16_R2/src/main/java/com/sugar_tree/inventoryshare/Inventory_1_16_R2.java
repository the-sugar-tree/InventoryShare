package com.sugar_tree.inventoryshare;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.Inventory;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.NonNullList;
import net.minecraft.server.v1_16_R2.PlayerInventory;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.sugar_tree.inventoryshare.FileManager_1_16_R2.*;
import static com.sugar_tree.inventoryshare.api.Variables.plugin;
import static com.sugar_tree.inventoryshare.api.Variables.teaminventory;

public class Inventory_1_16_R2 implements Inventory {

    public void invApplyAll(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.inventory;
        try {
            setField(playerInventory, "items", items);
            setField(playerInventory, "armor", armor);
            setField(playerInventory, "extraSlots", extraSlots);
            setField(playerInventory, "f", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void invDisApply(@NotNull Player p) {
        EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
        PlayerInventory playerInventory = entityPlayer.inventory;
        if (invList.containsKey(entityPlayer.getUniqueID())) {
            try {
                NonNullList<ItemStack> items1 = invList.get(entityPlayer.getUniqueID()).items;
                NonNullList<ItemStack> armor1 = invList.get(entityPlayer.getUniqueID()).armor;
                NonNullList<ItemStack> extraSlots1 = invList.get(entityPlayer.getUniqueID()).extraSlots;
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
                NonNullList<ItemStack> items1 = NonNullList.a(36, ItemStack.b);
                NonNullList<ItemStack> armor1 = NonNullList.a(4, ItemStack.b);
                NonNullList<ItemStack> extraSlots1 = NonNullList.a(1, ItemStack.b);
                List<NonNullList<ItemStack>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                setField(playerInventory, "items", items1);
                setField(playerInventory, "armor", armor1);
                setField(playerInventory, "extraSlots", extraSlots1);
                setField(playerInventory, "f", contents1);
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
        invList.put(p.getUniqueId(), pinv);
    }

    public Set<UUID> getRegisteredPlayers() {
        return invList.keySet();
    }
}

