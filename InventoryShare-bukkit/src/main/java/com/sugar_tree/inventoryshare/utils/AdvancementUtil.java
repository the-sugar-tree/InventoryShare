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

package com.sugar_tree.inventoryshare.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.sugar_tree.inventoryshare.SharedConstants.advancement;
import static com.sugar_tree.inventoryshare.SharedConstants.advlist;

public class AdvancementUtil {

    public static void AdvancementPatch(Player player) {
        if (!advancement) {
            return;
        }
        Iterator<Advancement> serveradvancements = Bukkit.getServer().advancementIterator();
        while (serveradvancements.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(serveradvancements.next());
            if (!progress.isDone()) {
                continue;
            }
            if (!(advlist.contains(progress.getAdvancement().getKey()))) {
                advlist.add(progress.getAdvancement().getKey());
            }
        }
        for (NamespacedKey namespacedKey : advlist) {
            Advancement adv = Bukkit.getServer().getAdvancement(namespacedKey);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getAdvancementProgress(adv).isDone()) {
                    continue;
                }
                AdvancementProgress progress = p.getAdvancementProgress(adv);
                List<String> crl = new ArrayList<>(progress.getRemainingCriteria());
                for (String cr : crl) {
                    progress.awardCriteria(cr);
                }
            }
        }
    }
}
