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

package com.sugar_tree.inventoryshare.inventory;

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

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class InventoryManager implements IInventoryManager {

    private final InventoryAccess access;
    private final InventoryRegistry registry;

    /**
     * Team whose inventory each player in {@code TEAM} state is using
     */
    private final Map<UUID, String> appliedTeams = new HashMap<>();

    public InventoryManager(InventoryAccess access, InventoryRegistry registry) {
        this.access = access;
        this.registry = registry;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void updateInventroy(@NotNull final Player p) {
        if (!inventory) {
            applyPersonalInventory(p);
            return;
        }
        if (!teaminventory) {
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
        if (InventoryState.getInventoryState(p).equals(InventoryState.ALL)) return;
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        try {
            access.apply(p, registry.getSharedInventory());
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.ALL);
        appliedTeams.remove(p.getUniqueId());
        p.updateInventory();
    }

    @Override
    public void applyTeamInventory(@NotNull final Player p, @NotNull final String teamName) {
        // Moving to another team must apply that team's inventory, so only the same team is skipped
        if (InventoryState.getInventoryState(p).equals(InventoryState.TEAM)
                && teamName.equals(appliedTeams.get(p.getUniqueId()))) return;
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        InventoryStorage teamInventory = registry.getTeamInventory(teamName);
        if (teamInventory == null) {
            teamInventory = access.createEmpty();
            registry.putTeamInventory(teamName, teamInventory);
        }
        try {
            access.apply(p, teamInventory);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.TEAM);
        appliedTeams.put(p.getUniqueId(), teamName);
        p.updateInventory();
    }

    @Override
    public void applyPersonalInventory(@NotNull final Player p) {
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) return;
        InventoryStorage personalInventory = registry.getPlayerInventory(p);
        if (personalInventory != null) {
            try {
                access.apply(p, personalInventory);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            registry.removePlayerInventory(p);
        }
        InventoryState.setInventoryState(p, InventoryState.PERSONAL);
        appliedTeams.remove(p.getUniqueId());
    }

    @Override
    public void savePlayerInventory(@NotNull final Player p) {
        if (!InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL))
            throw new IllegalStateException("Currently, the player <" + p.getName() + "> is not in a personal inventory state!");
        registry.putPlayerInventory(p, access.capture(p));
        InventoryState.setInventoryState(p, InventoryState.SAVED);
    }

    public @NotNull Set<UUID> getRegisteredPlayers() {
        return registry.getSavedPlayers();
    }

    @Override
    public void forgetPlayer(@NotNull final Player p) {
        InventoryState.forget(p);
        appliedTeams.remove(p.getUniqueId());
    }

    private enum InventoryState {
        /**
         * saved and {@link #PERSONAL} inventory state, this state means the player in this state should be applied another inventory now
         */
        SAVED,
        TEAM,
        ALL,
        PERSONAL;
        /** Keyed by UUID: a rejoining player gets a new {@link Player} object */
        private static final Map<UUID, InventoryState> playerInventoryStateInfo = new HashMap<>();

        public static void setInventoryState(Player p, InventoryState state) {
            playerInventoryStateInfo.put(p.getUniqueId(), state);
        }

        public static InventoryState getInventoryState(Player p) {
            InventoryState state = playerInventoryStateInfo.get(p.getUniqueId());
            return state == null ? PERSONAL : state;
        }

        public static void forget(Player p) {
            playerInventoryStateInfo.remove(p.getUniqueId());
        }
    }

}
