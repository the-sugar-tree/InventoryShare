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

package com.sugar_tree.inventoryshare.inventory;

import com.sugar_tree.inventoryshare.PluginSettings;
import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class InventoryManager implements IInventoryManager {

    private final InventoryAccess access;
    private final InventoryRegistry registry;
    private final PluginSettings settings;

    /**
     * Team whose inventory each player in {@code TEAM} state is using
     */
    private final Map<UUID, String> appliedTeams = new HashMap<>();

    /**
     * Inventory state of each player, keyed by UUID: a rejoining player gets a new {@link Player} object.
     * A player without an entry is in {@code PERSONAL} state.
     */
    private final Map<UUID, InventoryState> states = new HashMap<>();

    public InventoryManager(InventoryAccess access, InventoryRegistry registry, PluginSettings settings) {
        this.access = access;
        this.registry = registry;
        this.settings = settings;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void updateInventory(@NotNull final Player p) {
        if (!settings.isInventory()) {
            applyPersonalInventory(p);
            return;
        }
        if (!settings.isTeaminventory()) {
            applyAllInventory(p);
            return;
        }
        @Nullable Team team = Bukkit.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p);
        if (team == null) {
            applyAllInventory(p);
            return;
        }
        String teamName = team.getName();
        applyTeamInventory(p, teamName);
    }

    @Override
    public void applyAllInventory(@NotNull final Player p) {
        InventoryState previous = getState(p);
        if (previous.equals(InventoryState.ALL)) return;
        if (previous.equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        try {
            access.apply(p, registry.getSharedInventory());
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Could not apply the shared inventory to " + p.getName()
                    + "; they keep the inventory they were using", e);
            // The player still uses their own inventory, so the one saved above is dropped
            if (previous.equals(InventoryState.PERSONAL)) registry.removePlayerInventory(p);
            setState(p, previous);
            return;
        }
        setState(p, InventoryState.ALL);
        appliedTeams.remove(p.getUniqueId());
        p.updateInventory();
    }

    @Override
    public void applyTeamInventory(@NotNull final Player p, @NotNull final String teamName) {
        InventoryState previous = getState(p);
        // Moving to another team must apply that team's inventory, so only the same team is skipped
        if (previous.equals(InventoryState.TEAM)
                && teamName.equals(appliedTeams.get(p.getUniqueId()))) return;
        if (previous.equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        InventoryStorage teamInventory = registry.getTeamInventory(teamName);
        if (teamInventory == null) {
            teamInventory = access.createEmpty();
            registry.putTeamInventory(teamName, teamInventory);
        }
        try {
            access.apply(p, teamInventory);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Could not apply the inventory of team " + teamName + " to " + p.getName()
                    + "; they keep the inventory they were using", e);
            // The player still uses their own inventory, so the one saved above is dropped
            if (previous.equals(InventoryState.PERSONAL)) registry.removePlayerInventory(p);
            setState(p, previous);
            return;
        }
        setState(p, InventoryState.TEAM);
        appliedTeams.put(p.getUniqueId(), teamName);
        p.updateInventory();
    }

    @Override
    public void applyPersonalInventory(@NotNull final Player p) {
        if (getState(p).equals(InventoryState.PERSONAL)) return;
        InventoryStorage personalInventory = registry.getPlayerInventory(p);
        if (personalInventory != null) {
            try {
                access.apply(p, personalInventory);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Could not give " + p.getName() + " their own inventory back"
                        + "; they keep the inventory they were using", e);
                // Their own inventory stays saved for the next attempt
                return;
            }
            registry.removePlayerInventory(p);
        }
        setState(p, InventoryState.PERSONAL);
        appliedTeams.remove(p.getUniqueId());
    }

    @Override
    public void savePlayerInventory(@NotNull final Player p) {
        if (!getState(p).equals(InventoryState.PERSONAL))
            throw new IllegalStateException("Currently, the player <" + p.getName() + "> is not in a personal inventory state!");
        registry.putPlayerInventory(p, access.capture(p));
        setState(p, InventoryState.SAVED);
    }

    public @NotNull Set<UUID> getRegisteredPlayers() {
        return registry.getSavedPlayers();
    }

    @Override
    public void forgetPlayer(@NotNull final Player p) {
        states.remove(p.getUniqueId());
        appliedTeams.remove(p.getUniqueId());
    }

    private InventoryState getState(Player p) {
        InventoryState state = states.get(p.getUniqueId());
        return state == null ? InventoryState.PERSONAL : state;
    }

    private void setState(Player p, InventoryState state) {
        states.put(p.getUniqueId(), state);
    }

    private enum InventoryState {
        /**
         * The player's own inventory has just been saved, and another inventory is about to be applied
         */
        SAVED,
        TEAM,
        ALL,
        PERSONAL
    }

}
