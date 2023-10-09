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

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.utils.I18NUtil;
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

import static com.sugar_tree.inventoryshare.utils.AdvancementUtil.AdvancementPatch;
import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class Listeners implements Listener {

    private final int taskId;

    public int getTaskId() {
        return taskId;
    }

    public Listeners() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 0L).getTaskId();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(PREFIX + ChatColor.YELLOW + "This server is using \""+ I18NUtil.get("plugin_name")
                + ChatColor.YELLOW + "\" by." + ChatColor.GREEN + "sugar_tree");
        InventoryManager.savePlayerInventory(event.getPlayer());
        if (inventory) InventoryManager.updateInventroy(event.getPlayer());
        AdvancementPatch(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        InventoryManager.applyPersonalInventory(event.getPlayer());
        FileManager.save();
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        // FIXME: 3번 저장되는 오류 수정

        FileManager.save();
        if (inventory) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                InventoryManager.applyPersonalInventory(p);
                p.saveData();
                InventoryManager.updateInventroy(p);
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
            Location deathLocation = event.getEntity().getLocation();
            World deathWorld = deathLocation.getWorld();
            Bukkit.broadcastMessage(PREFIX + I18NUtil.get("death_info", event.getEntity().getName(), (deathWorld == null ? null : deathWorld.getName()),
                    deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ()));
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
                            InventoryManager.updateInventroy(p);
                        }
                    }
                }
            }
            teamMap.put(p, Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
        }
    }
}
