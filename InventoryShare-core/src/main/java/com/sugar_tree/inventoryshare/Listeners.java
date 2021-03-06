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
package com.sugar_tree.inventoryshare;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.sugar_tree.inventoryshare.Advancement.AdvancementPatch;
import static com.sugar_tree.inventoryshare.api.Variables.*;

public class Listeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text(PREFIX + ChatColor.YELLOW + "This server is using \"인벤토리 공유 플러그인\" by." + ChatColor.GREEN + "sugar_tree"));
        InventoryClass.savePlayerInventory(event.getPlayer());
        if (inventory) InventoryClass.invApply(event.getPlayer());
        AdvancementPatch(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        InventoryClass.invDisApply(event.getPlayer());
        fileManagerClass.save();
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event){
        fileManagerClass.save();
        if (inventory) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                InventoryClass.invDisApply(p);
                p.saveData();
                InventoryClass.invApply(p);
            }
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!(advlist.contains(event.getAdvancement().getKey()))) {
            advlist.add(event.getAdvancement().getKey());
        }
        if (advancement) {
            AdvancementPatch(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (AnnounceDeath) {
            Bukkit.broadcast(Component.text(PREFIX + ChatColor.RED + event.getEntity().getName() + "(이)가 [" + event.getEntity().getLocation().getWorld().getKey().getKey() + "] x: "
                    + event.getEntity().getLocation().getBlockX() + ", y: " + event.getEntity().getLocation().getBlockY() + ", z: " + event.getEntity().getLocation().getBlockZ()
                    + "에서 사망했습니다."));
        }
    }

    Map<Player, Team> teamMap = new HashMap<>();
    @EventHandler(priority = EventPriority.LOW)
    public void onTick(ServerTickEndEvent event) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamMap.containsKey(p)) {
                if (inventory) {
                    if (teaminventory) {
                        if (!Objects.equals(Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p), teamMap.get(p))) {
                            teamMap.put(p, Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
                            InventoryClass.invApply(p);
                        }
                    }
                }
            }
            teamMap.put(p, Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
        }
    }
}
