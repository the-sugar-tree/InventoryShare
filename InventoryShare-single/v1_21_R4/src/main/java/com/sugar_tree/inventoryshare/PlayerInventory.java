package com.sugar_tree.inventoryshare;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;

@Getter
@RequiredArgsConstructor
public class PlayerInventory {
    private final NonNullList<ItemStack> items;
    private final EnumMap<EnumItemSlot, ItemStack> equipment;

    public PlayerInventory(NonNullList<ItemStack> items) {
        this(items, new EnumMap<>(EnumItemSlot.class));
    }
}
