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

package com.sugar_tree.inventoryshare.nms.legacy;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsLayout;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * NMS member names for the {@code items / armor / extraSlots / contents} inventory model (1.12 - 1.21.4).
 */
@Getter
public final class LegacyNames implements NmsLayout {

    private static final String CREATE_ITEM_LIST = "a";

    /** {@code vX_Y_RZ} package of CraftBukkit (and of NMS before 1.17) */
    private final String craftbukkitPackage;
    /** 1.17+: NMS packages are flattened and the player inventory is reached through a method */
    private final boolean flattened;
    /** inventory field (pre-1.17) or method (1.17+) on the player */
    private final String inventory;
    private final String items;
    private final String armor;
    private final String extraSlots;
    private final String contents;
    private final String emptyItem;

    private LegacyNames(String craftbukkitPackage, boolean flattened, String inventory, String items,
                        String armor, String extraSlots, String contents, String emptyItem) {
        this.craftbukkitPackage = craftbukkitPackage;
        this.flattened = flattened;
        this.inventory = inventory;
        this.items = items;
        this.armor = armor;
        this.extraSlots = extraSlots;
        this.contents = contents;
        this.emptyItem = emptyItem;
    }

    /** Pre-1.17: {@code net.minecraft.server.vX_Y_RZ} packages, inventory is a field. */
    public static LegacyNames legacy(String craftbukkitPackage, String inventory, String items, String armor,
                                     String extraSlots, String contents, String emptyItem) {
        return new LegacyNames(craftbukkitPackage, false, inventory, items, armor, extraSlots, contents, emptyItem);
    }

    /** 1.17 - 1.21.4: flattened NMS packages, inventory is a method. */
    public static LegacyNames flattened(String craftbukkitPackage, String inventory, String items, String armor,
                                        String extraSlots, String contents, String emptyItem) {
        return new LegacyNames(craftbukkitPackage, true, inventory, items, armor, extraSlots, contents, emptyItem);
    }

    public String getCreateItemList() {
        return CREATE_ITEM_LIST;
    }

    public String getCraftPlayerClass() {
        return "org.bukkit.craftbukkit." + craftbukkitPackage + ".entity.CraftPlayer";
    }

    public String getCraftItemStackClass() {
        return "org.bukkit.craftbukkit." + craftbukkitPackage + ".inventory.CraftItemStack";
    }

    /** Class declaring the {@link #getInventory() inventory} member. */
    public String getInventoryOwnerClass() {
        return flattened ? "net.minecraft.server.level.EntityPlayer" : nms("EntityHuman");
    }

    public String getPlayerInventoryClass() {
        return flattened ? "net.minecraft.world.entity.player.PlayerInventory" : nms("PlayerInventory");
    }

    public String getItemStackClass() {
        return flattened ? "net.minecraft.world.item.ItemStack" : nms("ItemStack");
    }

    public String getNonNullListClass() {
        return flattened ? "net.minecraft.core.NonNullList" : nms("NonNullList");
    }

    private String nms(String simpleName) {
        return "net.minecraft.server." + craftbukkitPackage + "." + simpleName;
    }

    @Override
    public @NotNull InventoryAccess createAccess(@NotNull ClassLoader loader) throws ReflectiveOperationException {
        return new LegacyInventoryAccess(LegacyMembers.resolve(this, loader));
    }
}
