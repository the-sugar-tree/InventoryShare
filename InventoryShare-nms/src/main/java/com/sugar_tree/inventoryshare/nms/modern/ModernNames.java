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

package com.sugar_tree.inventoryshare.nms.modern;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsLayout;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NMS member names for the {@code items + EntityEquipment} inventory model (1.21.5+).
 */
@Getter
public final class ModernNames implements NmsLayout {

    private final Namespace namespace;
    /** {@code vX_Y_RZ} package of CraftBukkit, or {@code null} once CraftBukkit stopped relocating (26.1+) */
    private final @Nullable String craftbukkitPackage;
    /** {@code Player#getInventory()} */
    private final String getInventory;
    /** {@code Inventory#items} */
    private final String items;
    /** {@code Inventory#equipment} */
    private final String equipment;
    /** {@code EntityEquipment#items} */
    private final String equipmentItems;
    /** {@code Inventory#EQUIPMENT_SLOT_MAPPING}: slot index (36 - 40) to EquipmentSlot */
    private final String slotMapping;
    /** {@code ItemStack#EMPTY} */
    private final String emptyItem;
    /** {@code NonNullList#withSize(int, Object)} */
    private final String createItemList;

    private ModernNames(Namespace namespace, @Nullable String craftbukkitPackage, String getInventory, String items,
                        String equipment, String equipmentItems, String slotMapping, String emptyItem,
                        String createItemList) {
        this.namespace = namespace;
        this.craftbukkitPackage = craftbukkitPackage;
        this.getInventory = getInventory;
        this.items = items;
        this.equipment = equipment;
        this.equipmentItems = equipmentItems;
        this.slotMapping = slotMapping;
        this.emptyItem = emptyItem;
        this.createItemList = createItemList;
    }

    /** 1.21.5 - 1.21.11: Spigot-obfuscated member names. */
    public static ModernNames spigot(String craftbukkitPackage, String getInventory, String items, String equipment,
                                     String equipmentItems, String slotMapping, String emptyItem,
                                     String createItemList) {
        return new ModernNames(Namespace.SPIGOT, craftbukkitPackage, getInventory, items, equipment,
                equipmentItems, slotMapping, emptyItem, createItemList);
    }

    /** 26.1+: Minecraft is no longer obfuscated, members keep Mojang's names. */
    public static ModernNames mojang() {
        return new ModernNames(Namespace.MOJANG, null, "getInventory", "items", "equipment",
                "items", "EQUIPMENT_SLOT_MAPPING", "EMPTY", "withSize");
    }

    public String getCraftPlayerClass() {
        return craftbukkit("entity.CraftPlayer");
    }

    public String getCraftItemStackClass() {
        return craftbukkit("inventory.CraftItemStack");
    }

    private String craftbukkit(String name) {
        return craftbukkitPackage == null
                ? "org.bukkit.craftbukkit." + name
                : "org.bukkit.craftbukkit." + craftbukkitPackage + "." + name;
    }

    @Override
    public @NotNull InventoryAccess createAccess(@NotNull ClassLoader loader) throws ReflectiveOperationException {
        return new ModernInventoryAccess(ModernMembers.resolve(this, loader));
    }
}
