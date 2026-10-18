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

package com.sugar_tree.inventoryshare.nms;

/**
 * Thrown when the version is supported, but the {@link InventoryAccess} for this server cannot be created: the
 * server's internals differ from what the plugin expects (wrong names, an unexpected server build or fork).
 * This is a bug to report, unlike {@link UnsupportedVersionException}.
 */
public class IncompatibleServerException extends Exception {

    public IncompatibleServerException(String message) {
        super(message);
    }

    public IncompatibleServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
