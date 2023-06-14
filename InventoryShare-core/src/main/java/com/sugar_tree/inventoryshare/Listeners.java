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

import com.sugar_tree.inventoryshare.util.I18NUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

import static com.sugar_tree.inventoryshare.util.AdvancementUtil.AdvancementPatch;
import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public class Listeners implements Listener {

    private final int taskId;

    public int getTaskId() {
        return taskId;
    }

    public Listeners() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 0L).getTaskId();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(PREFIX + ChatColor.YELLOW + "This server is using \""+ I18NUtil.get("plugin_name") +"\" by." + ChatColor.GREEN + "sugar_tree");
        InventoryManager.savePlayerInventory(event.getPlayer());
        if (inventory) InventoryManager.applyInventory(event.getPlayer());
        AdvancementPatch(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        InventoryManager.disApplyInventory(event.getPlayer());
        FileManager.save();
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event){
        FileManager.save();
        if (inventory) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                InventoryManager.disApplyInventory(p);
                p.saveData();
                InventoryManager.applyInventory(p);
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
        if (announcedeath) {
            Location loc = event.getEntity().getLocation();
            World w = loc.getWorld();
            Bukkit.broadcastMessage(PREFIX + I18NUtil.get("death_info", event.getEntity().getName(), (w == null ? null : w.getName()),
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
    }

    private final Map<Player, Team> teamMap = new HashMap<>();
    @SuppressWarnings("deprecation")
    public void onTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamMap.containsKey(p)) {
                if (inventory) {
                    if (teaminventory) {
                        if (!Objects.equals(Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p), teamMap.get(p))) {
                            teamMap.put(p, Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
                            InventoryManager.applyInventory(p);
                        }
                    }
                }
            }
            teamMap.put(p, Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
        }
    }
}
