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

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.utils.AdvancementRegistry;
import com.sugar_tree.inventoryshare.utils.I18NUtil;
import lombok.Getter;
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
import java.util.UUID;

import static com.sugar_tree.inventoryshare.utils.AdvancementUtil.AdvancementPatch;
import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class Listeners implements Listener {

    @Getter
    private final int taskId;

    private final PluginSettings settings;
    private final IInventoryManager inventoryManager;
    private final IFileManager fileManager;
    private final AdvancementRegistry advancements;

    public Listeners(PluginSettings settings, IInventoryManager inventoryManager, IFileManager fileManager,
                     AdvancementRegistry advancements) {
        this.settings = settings;
        this.inventoryManager = inventoryManager;
        this.fileManager = fileManager;
        this.advancements = advancements;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 0L).getTaskId();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(PREFIX + ChatColor.YELLOW + "This server is using \""+ I18NUtil.get("plugin_name")
                + ChatColor.YELLOW + "\" by." + ChatColor.GREEN + "sugar_tree");
        inventoryManager.updateInventory(event.getPlayer());
        AdvancementPatch(event.getPlayer(), settings, advancements);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        inventoryManager.applyPersonalInventory(player);
        inventoryManager.forgetPlayer(player);
        teamMap.remove(player.getUniqueId());
        fileManager.save();
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        // Called once per world; the server saves its players and every world together, so the main world is enough
        if (!event.getWorld().equals(Bukkit.getWorlds().get(0))) return;

        fileManager.save();
        if (settings.isInventory()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                inventoryManager.applyPersonalInventory(p);
                p.saveData();
                inventoryManager.updateInventory(p);
            }
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        advancements.add(event.getAdvancement().getKey());
        if (settings.isAdvancement()) {
            AdvancementPatch(event.getPlayer(), settings, advancements);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (settings.isAnnouncedeath()) {
            Location deathLocation = event.getEntity().getLocation();
            World deathWorld = deathLocation.getWorld();
            Bukkit.broadcastMessage(PREFIX + I18NUtil.get("death_info", event.getEntity().getName(), (deathWorld == null ? null : deathWorld.getName()),
                    deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ()));
        }
    }

    /** Last seen team of each online player, keyed by UUID; removed when the player quits */
    private final Map<UUID, Team> teamMap = new HashMap<>();
    /** Runs every tick: while inventory sharing and team inventories are on, updates the inventory of each player whose team changed */
    @SuppressWarnings("deprecation")
    public void onTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamMap.containsKey(p.getUniqueId())) {
                if (settings.isInventory()) {
                    if (settings.isTeaminventory()) {
                        if (!Objects.equals(Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p), teamMap.get(p.getUniqueId()))) {
                            teamMap.put(p.getUniqueId(), Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
                            inventoryManager.updateInventory(p);
                        }
                    }
                }
            }
            teamMap.put(p.getUniqueId(), Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(p));
        }
    }
}
