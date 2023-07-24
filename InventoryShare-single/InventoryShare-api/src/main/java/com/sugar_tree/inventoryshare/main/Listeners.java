/*
 * This file is part of the-sugar-tree, licensed under the GPL-3.0 License.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.main;

import com.sugar_tree.inventoryshare.main.util.I18NUtil;
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

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;
import static com.sugar_tree.inventoryshare.main.util.AdvancementUtil.AdvancementPatch;

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
