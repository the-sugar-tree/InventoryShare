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

package com.sugar_tree.inventoryshare.utils;

import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Advancements that someone has completed, which are shared with every player
 */
public final class AdvancementRegistry {

    private final Set<NamespacedKey> advancements = new LinkedHashSet<>();

    /** Does nothing if the advancement is already in the list */
    public void add(NamespacedKey advancement) {
        advancements.add(advancement);
    }

    public void remove(NamespacedKey advancement) {
        advancements.remove(advancement);
    }

    /** @return the advancements in the order they were added */
    public Set<NamespacedKey> getAdvancements() {
        return Collections.unmodifiableSet(advancements);
    }
}
