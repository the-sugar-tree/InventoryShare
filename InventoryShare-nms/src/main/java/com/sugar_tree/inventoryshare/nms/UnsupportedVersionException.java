/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with other players
 * Copyright (c) 2023 the-sugar-tree and contributors
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

package com.sugar_tree.inventoryshare.nms;

/**
 * Thrown when no {@link InventoryAccessProvider} supports the Minecraft version: the plugin does not support it.
 * Not a bug; a supported version that fails is an {@link IncompatibleServerException}.
 */
public class UnsupportedVersionException extends Exception {

    public UnsupportedVersionException(String message) {
        super(message);
    }
}
