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

package com.sugar_tree.inventoryshare.nms;

import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
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
    private final NmsInventory sharedInventory;
    private final Map<String, NmsInventory> teamInventoryMap = new HashMap<>();

    /**
     * UUID - Player's UUID <br>
     * NmsInventory - the player's own inventory, saved while a shared one is applied
     */
    private final Map<UUID, NmsInventory> originalPlayerInventoryMap = new HashMap<>();

    public InventoryManager(InventoryAccess access) {
        this.access = access;
        this.sharedInventory = access.createEmpty();
    }

    NmsInventory getSharedInventory() {
        return sharedInventory;
    }

    Map<String, NmsInventory> getTeamInventoryMap() {
        return teamInventoryMap;
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
            access.apply(p, sharedInventory);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.ALL);
        p.updateInventory();
    }

    @Override
    public void applyTeamInventory(@NotNull final Player p, @NotNull final String teamName) {
        if (InventoryState.getInventoryState(p).equals(InventoryState.TEAM)) return;
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        NmsInventory teamInventory = teamInventoryMap.get(teamName);
        if (teamInventory == null) {
            teamInventory = access.createEmpty();
            teamInventoryMap.put(teamName, teamInventory);
        }
        try {
            access.apply(p, teamInventory);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.TEAM);
        p.updateInventory();
    }

    @Override
    public void applyPersonalInventory(@NotNull final Player p) {
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) return;
        if (originalPlayerInventoryMap.containsKey(p.getUniqueId())) {
            try {
                access.apply(p, originalPlayerInventoryMap.get(p.getUniqueId()));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            originalPlayerInventoryMap.remove(p.getUniqueId());
        }
        InventoryState.setInventoryState(p, InventoryState.PERSONAL);
    }

    @Override
    public void savePlayerInventory(@NotNull final Player p) {
        if (!InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL))
            throw new IllegalStateException("Currently, the player <" + p.getName() + "> is not in a personal inventory state!");
        originalPlayerInventoryMap.put(p.getUniqueId(), access.capture(p));
        InventoryState.setInventoryState(p, InventoryState.SAVED);
    }

    public @NotNull Set<UUID> getRegisteredPlayers() {
        return originalPlayerInventoryMap.keySet();
    }

    private enum InventoryState {
        /**
         * saved and {@link #PERSONAL} inventory state, this state means the player in this state should be applied another inventory now
         */
        SAVED,
        TEAM,
        ALL,
        PERSONAL;
        private static final Map<Player, InventoryState> playerInventoryStateInfo = new HashMap<>();

        public static void setInventoryState(Player p, InventoryState state) {
            playerInventoryStateInfo.put(p, state);
        }

        public static InventoryState getInventoryState(Player p) {
            return playerInventoryStateInfo.get(p) == null ? PERSONAL : playerInventoryStateInfo.get(p);
        }
    }

}
