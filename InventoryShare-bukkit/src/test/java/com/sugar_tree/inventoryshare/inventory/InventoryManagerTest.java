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
import com.sugar_tree.inventoryshare.SharedConstants;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryManagerTest {

    private final FakeAccess access = new FakeAccess();
    private final InventoryRegistry registry = new InventoryRegistry(access);
    private final InventoryManager manager = new InventoryManager(access, registry, new PluginSettings(null));
    private final Player player = player();

    @BeforeAll
    static void setUpLogger() {
        Logger logger = Logger.getLogger("InventoryManagerTest");
        logger.setUseParentHandlers(false);
        SharedConstants.logger = logger;
    }

    @Test
    void failedSwitchFromOwnInventoryKeepsIt() {
        access.failing = true;
        manager.applyAllInventory(player);

        assertEquals(Collections.emptySet(), manager.getRegisteredPlayers(), "the own inventory must not stay saved");
        access.failing = false;
        manager.applyAllInventory(player);
        assertEquals(2, access.captures, "still in personal state, so the own inventory is saved again");
        assertSame(registry.getSharedInventory(), access.applied.get(access.applied.size() - 1));
    }

    @Test
    void failedTeamSwitchKeepsTheSharedInventory() {
        manager.applyAllInventory(player);
        access.failing = true;
        manager.applyTeamInventory(player, "red");

        access.failing = false;
        manager.applyAllInventory(player);
        assertEquals(1, access.applied.size(), "still in shared state, so applying it again does nothing");
        assertEquals(Collections.singleton(player.getUniqueId()), manager.getRegisteredPlayers());
    }

    @Test
    void failedReturnKeepsTheOwnInventorySaved() {
        manager.applyAllInventory(player);
        InventoryStorage own = registry.getPlayerInventory(player);
        access.failing = true;
        manager.applyPersonalInventory(player);

        assertSame(own, registry.getPlayerInventory(player));
        access.failing = false;
        manager.applyPersonalInventory(player);
        assertSame(own, access.applied.get(access.applied.size() - 1), "the return is tried again");
        assertTrue(manager.getRegisteredPlayers().isEmpty());
    }

    private static Player player() {
        UUID uuid = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUniqueId": return uuid;
                        case "getName": return "player";
                        case "updateInventory": return null;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** Records the applied storages; while {@link #failing}, every apply throws without changing anything */
    private static final class FakeAccess implements InventoryAccess {
        boolean failing;
        int captures;
        final List<InventoryStorage> applied = new ArrayList<>();

        @Override
        public InventoryStorage createEmpty() {
            return new InventoryStorage() {};
        }

        @Override
        public InventoryStorage capture(Player player) {
            captures++;
            return new InventoryStorage() {};
        }

        @Override
        public void apply(Player player, InventoryStorage inventory) {
            if (failing) throw new IllegalStateException("apply failed");
            applied.add(inventory);
        }

        @Override
        public ItemStack getItem(InventoryStorage inventory, int slot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setItem(InventoryStorage inventory, int slot, ItemStack item) {
            throw new UnsupportedOperationException();
        }
    }
}
