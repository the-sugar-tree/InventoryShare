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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.sugar_tree.inventoryshare.SharedConstants.plugin;
import static com.sugar_tree.inventoryshare.SharedConstants.teaminventory;
import static com.sugar_tree.inventoryshare.nms.NMSLoader.*;

public final class InventoryManager implements IInventoryManager {

    public InventoryManager() {}

    /**
     * UUID - Player's UUID <br>
     * Object - PlayerInventory
     */
    private final Map<UUID, Object> originalPlayerInventoryMap = new HashMap<>();

    @Override
    public void applyAllInventory(@NotNull Player p) {
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        try {
            fillAllInventoryFields(playerInventory, sharedItems, sharedArmor, sharedExtraSlots, sharedContents);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disApplyInventory(@NotNull Player p) {
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        if (originalPlayerInventoryMap.containsKey(p.getUniqueId())) {
            Object originalPlayerInventory = originalPlayerInventoryMap.get(p.getUniqueId());
            AbstractList<Object> items = NMSLoader.getInventoryItems(originalPlayerInventory);
            AbstractList<Object> armor = NMSLoader.getInventoryArmor(originalPlayerInventory);
            AbstractList<Object> extraSlots = NMSLoader.getInventoryExtraSlots(originalPlayerInventory);
            List<AbstractList<Object>> contents = ImmutableList.of(items, armor, extraSlots);
            try {
                fillAllInventoryFields(playerInventory, items, armor, extraSlots, contents);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            // 사용될 일이 없지만, 혹시 모른 버그 방지
            AbstractList<Object> items = NMSLoader.createEmptyItemList(36);
            AbstractList<Object> armor = NMSLoader.createEmptyItemList(4);
            AbstractList<Object> extraSlots = NMSLoader.createEmptyItemList(1);
            List<AbstractList<Object>> contents = ImmutableList.of(items, armor, extraSlots);
            try {
                fillAllInventoryFields(playerInventory, items, armor, extraSlots, contents);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        originalPlayerInventoryMap.remove(p.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void applyInventory(@NotNull Player p) {
        if (!(teaminventory)) {
            applyAllInventory(p);
            p.updateInventory();
            return;
        }
        if (plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p) == null) {
            applyAllInventory(p);
            p.updateInventory();
            return;
        }
        String teamName = plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p).getName();
        AbstractList<Object> itemsT;
        AbstractList<Object> armorT;
        AbstractList<Object> extraSlotsT;
        if (!TeamInventoryInfo.containsKey(teamName)) {
            Map<String, AbstractList<Object>> map = new HashMap<>();
            itemsT = NMSLoader.createEmptyItemList(36);
            armorT = NMSLoader.createEmptyItemList(4);
            extraSlotsT = NMSLoader.createEmptyItemList(1);
            map.put("items", itemsT);
            map.put("armor", armorT);
            map.put("extraSlots", extraSlotsT);
            TeamInventoryInfo.put(teamName, map);
        } else {
            Map<String, AbstractList<Object>> map = TeamInventoryInfo.get(teamName);
            itemsT = map.get("items");
            armorT = map.get("armor");
            extraSlotsT = map.get("extraSlots");
        }
        Object playerInventory = NMSLoader.getPlayerInventory(p);
        try {
            fillAllInventoryFields(playerInventory, itemsT, armorT, extraSlotsT);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        p.updateInventory();
    }

    @Override
    public void savePlayerInventory(@NotNull Player p) {
        Object copiedPlayerInventory = NMSLoader.createEmptyPlayerInventory();
        Object originalPlayerInventory = NMSLoader.getPlayerInventory(p);
        try {
            fillAllInventoryFields(copiedPlayerInventory, NMSLoader.getInventoryItems(originalPlayerInventory), NMSLoader.getInventoryArmor(originalPlayerInventory), NMSLoader.getInventoryExtraSlots(originalPlayerInventory));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        originalPlayerInventoryMap.put(p.getUniqueId(), copiedPlayerInventory);
    }

    public @NotNull Set<UUID> getRegisteredPlayers() {
        return originalPlayerInventoryMap.keySet();
    }

    private void fillAllInventoryFields(Object playerInventory, Object items, Object armor, Object extraSlots) throws NoSuchFieldException, IllegalAccessException {
        fillAllInventoryFields(playerInventory, items, armor, extraSlots, ImmutableList.of(items, armor, extraSlots));
    }

    private void fillAllInventoryFields(Object playerInventory, Object items, Object armor, Object extraSlots, Object contents) throws NoSuchFieldException, IllegalAccessException {
        final VersionUtil.SupportedVersions VERSION_INFO = VersionUtil.getVersion();
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_items(), items);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_armor(), armor);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_extraSlots(), extraSlots);
        setField(playerInventory, VERSION_INFO.getPATH_PlayerInventory_contents(), contents);
    }
}