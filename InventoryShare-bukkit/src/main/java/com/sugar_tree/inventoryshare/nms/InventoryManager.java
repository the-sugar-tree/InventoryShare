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

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.nms.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.sugar_tree.inventoryshare.SharedConstants.*;
import static com.sugar_tree.inventoryshare.nms.NMSLoader.*;

public final class InventoryManager implements IInventoryManager {

    public InventoryManager() {}

    /**
     * UUID - Player's UUID <br>
     * Object - PlayerInventory
     */
    private final Map<UUID, PlayerInventory> originalPlayerInventoryMap = new HashMap<>();

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
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        try {
            fillInventoryFields(playerInventory, sharedItems, sharedArmor, sharedExtraSlots, sharedContents);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.ALL);
        p.updateInventory();
    }

    @Override
    public void applyTeamInventory(@NotNull final Player p, @NotNull final String teamName) {
        if (InventoryState.getInventoryState(p).equals(InventoryState.TEAM)) return;
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) savePlayerInventory(p);
        AbstractList<Object> items;
        AbstractList<Object> armor;
        AbstractList<Object> extraSlots;
        if (!TeamInventoryMap.containsKey(teamName)) {
            // Create Empty Inventory
            items = NMSLoader.createEmptyItemList(36);
            armor = NMSLoader.createEmptyItemList(4);
            extraSlots = NMSLoader.createEmptyItemList(1);
            TeamInventoryMap.put(teamName, new PlayerInventory(items, armor, extraSlots));
        } else {
            PlayerInventory teamInventory = TeamInventoryMap.get(teamName);
            items = teamInventory.getItems();
            armor = teamInventory.getArmor();
            extraSlots = teamInventory.getExtraSlots();
        }
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        try {
            fillInventoryFields(playerInventory, items, armor, extraSlots);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        InventoryState.setInventoryState(p, InventoryState.TEAM);
        p.updateInventory();
    }

    @Override
    public void applyPersonalInventory(@NotNull final Player p) {
        if (InventoryState.getInventoryState(p).equals(InventoryState.PERSONAL)) return;
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        if (originalPlayerInventoryMap.containsKey(p.getUniqueId())) {
            PlayerInventory originalPlayerInventory = originalPlayerInventoryMap.get(p.getUniqueId());
            AbstractList<Object> items = originalPlayerInventory.getItems();
            AbstractList<Object> armor = originalPlayerInventory.getArmor();
            AbstractList<Object> extraSlots = originalPlayerInventory.getExtraSlots();
            try {
                fillInventoryFields(playerInventory, items, armor, extraSlots);
            } catch (NoSuchFieldException | IllegalAccessException e) {
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
        Object originalPlayerInventory = NMSLoader.getPlayerInventory(p);
        originalPlayerInventoryMap.put(p.getUniqueId(),
                new PlayerInventory(NMSLoader.getInventoryItems(originalPlayerInventory),
                        NMSLoader.getInventoryArmor(originalPlayerInventory),
                        NMSLoader.getInventoryExtraSlots(originalPlayerInventory)));
        InventoryState.setInventoryState(p, InventoryState.SAVED_BUT_NO_INVENTORY_TO_APPLY);
    }

    public @NotNull Set<UUID> getRegisteredPlayers() {
        return originalPlayerInventoryMap.keySet();
    }

    private void fillInventoryFields(Object playerInventory, Object items, Object armor, Object extraSlots)
            throws NoSuchFieldException, IllegalAccessException {
        fillInventoryFields(playerInventory, items, armor, extraSlots, ImmutableList.of(items, armor, extraSlots));
    }

    private void fillInventoryFields(Object playerInventory, Object items, Object armor, Object extraSlots, Object contents)
            throws NoSuchFieldException, IllegalAccessException {
        final VersionUtil.SupportedVersions VERSION_INFO = VersionUtil.getVersion();
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_items(), items);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_armor(), armor);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_extraSlots(), extraSlots);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_contents(), contents);
    }

    private enum InventoryState {
        SAVED_BUT_NO_INVENTORY_TO_APPLY, TEAM, ALL, PERSONAL;
        private static final Map<Player, InventoryState> playerInventoryStateInfo = new HashMap<>();

        public static void setInventoryState(Player p, InventoryState state) {
            playerInventoryStateInfo.put(p, state);
        }

        public static InventoryState getInventoryState(Player p) {
            return playerInventoryStateInfo.get(p) == null ? PERSONAL : playerInventoryStateInfo.get(p);
        }
    }
}