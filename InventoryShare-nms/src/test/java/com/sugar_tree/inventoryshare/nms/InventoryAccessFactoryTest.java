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

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryAccessFactoryTest {

    private static final ClassLoader LOADER = InventoryAccessFactoryTest.class.getClassLoader();
    private static final ServerRuntime SPIGOT_1_21_4 = new ServerRuntime("v1_21_R3", Namespace.SPIGOT, LOADER);

    @Test
    void providerOverridesMapping() throws UnsupportedVersionException {
        assertSame(TestProvider.ACCESS,
                InventoryAccessFactory.create(ServerVersion.parse(TestProvider.VERSION), SPIGOT_1_21_4));
    }

    @Test
    void unsupportedWithoutProviderOrMapping() {
        assertThrows(UnsupportedVersionException.class,
                () -> InventoryAccessFactory.create(ServerVersion.parse("1.11.2"), SPIGOT_1_21_4));
    }

    @Test
    void mappingFailsWithoutServerClasses() {
        // The 1.21.4 row exists, but this test classpath has no CraftBukkit/NMS classes
        assertThrows(UnsupportedVersionException.class,
                () -> InventoryAccessFactory.create(ServerVersion.parse("1.21.4"), SPIGOT_1_21_4));
    }

    /** Registered in {@code src/test/resources/META-INF/services}. */
    public static final class TestProvider implements InventoryAccessProvider {
        static final String VERSION = "99.0";
        static final InventoryAccess ACCESS = new InventoryAccess() {
            @Override
            public @NotNull NmsInventory createEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @NotNull NmsInventory capture(@NotNull Player player) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void apply(@NotNull Player player, @NotNull NmsInventory inventory) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @NotNull ItemStack getItem(@NotNull NmsInventory inventory, int slot) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setItem(@NotNull NmsInventory inventory, int slot, @NotNull ItemStack item) {
                throw new UnsupportedOperationException();
            }
        };

        @Override
        public InventoryAccess create(@NotNull ServerVersion version) {
            return ServerVersion.parse(VERSION).equals(version) ? ACCESS : null;
        }
    }
}
