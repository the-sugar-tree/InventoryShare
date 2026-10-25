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

import com.sugar_tree.inventoryshare.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.sugar_tree.inventoryshare.SharedConstants.logger;


public class AdvancementUtil {
    private AdvancementUtil() {}

    /**
     * Adds the advancements the player has completed to the shared list, then awards every advancement in the list
     * to all online players. Does nothing while advancement sharing is off.
     */
    public static void AdvancementPatch(Player player, PluginSettings settings, AdvancementRegistry advancements) {
        if (!settings.isAdvancement()) {
            return;
        }
        Iterator<Advancement> serveradvancements = Bukkit.getServer().advancementIterator();
        while (serveradvancements.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(serveradvancements.next());
            if (!progress.isDone()) {
                continue;
            }
            advancements.add(progress.getAdvancement().getKey());
        }
        // A copy, because advancements the server no longer has are removed while iterating
        for (NamespacedKey namespacedKey : new ArrayList<>(advancements.getAdvancements())) {
            Advancement adv = Bukkit.getServer().getAdvancement(namespacedKey);
            if (adv == null) {
                // Its data pack was disabled or reloaded away while the server was running
                logger.info("Advancement " + namespacedKey + " no longer exists; removed it from the shared list");
                advancements.remove(namespacedKey);
                continue;
            }
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
