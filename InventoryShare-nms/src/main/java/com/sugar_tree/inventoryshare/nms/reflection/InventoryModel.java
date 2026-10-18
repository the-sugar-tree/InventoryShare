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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where a Minecraft version stores the player's armor and offhand. Decided by the version alone; which names the
 * server uses for the classes and members is a separate question, see {@link NmsNameFactory}.
 */
public enum InventoryModel {
    /** 1.12 - 1.21.4: {@code armor} and {@code extraSlots} lists in the player inventory */
    LEGACY,
    /** 1.21.5+: the {@code EntityEquipment} of the player, referenced by the player inventory */
    MODERN,
    ;

    /**
     * @return the model of the version, or {@code null} if the version is not supported
     */
    public static @Nullable InventoryModel of(@NotNull MinecraftVersion version) {
        if (LegacyNameTable.find(version) != null) return LEGACY;
        if (ModernNameTable.find(version) != null) return MODERN;
        return null;
    }
}
