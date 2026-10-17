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
 * Which names the running server uses for NMS classes and members.
 *
 * <ul>
 *   <li>{@link #SPIGOT}: Spigot's mappings (obfuscated member names from 1.17) — every Spigot server up to 1.21.11,
 *       and Paper up to 1.20.4</li>
 *   <li>{@link #MOJANG}: Mojang's names — Paper 1.20.5+, Paper's Mojang-mapped 1.20.4 build, and every 26.1+ server</li>
 * </ul>
 *
 * @see ServerRuntime
 */
public enum Namespace {
    SPIGOT,
    MOJANG,
}
