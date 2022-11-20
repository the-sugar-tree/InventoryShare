/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sugar_tree.inventoryshare.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.sugar_tree.inventoryshare.api.SharedConstants.advancement;
import static com.sugar_tree.inventoryshare.api.SharedConstants.advlist;

public class AdvancementUtil {

    public static void AdvancementPatch(Player player) {
        if (advancement) {
            Iterator<Advancement> serveradvancements = Bukkit.getServer().advancementIterator();
            while (serveradvancements.hasNext()) {
                AdvancementProgress progress = player.getAdvancementProgress(serveradvancements.next());
                if (progress.isDone()) {
                    if (!(advlist.contains(progress.getAdvancement().getKey()))) {
                        advlist.add(progress.getAdvancement().getKey());
                    }
                }
            }
            for (NamespacedKey namespacedKey : advlist) {
                Advancement adv = Bukkit.getServer().getAdvancement(namespacedKey);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!(p.getAdvancementProgress(adv).isDone())) {
                        AdvancementProgress progress = p.getAdvancementProgress(adv);
                        List<String> crl = new ArrayList<>(progress.getRemainingCriteria());
                        for (String cr : crl) {
                            progress.awardCriteria(cr);
                        }
                    }
                }
            }
        }
    }
}
