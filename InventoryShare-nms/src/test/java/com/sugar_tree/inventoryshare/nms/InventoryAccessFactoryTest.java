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

import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryAccessFactoryTest {

    private static final ClassLoader LOADER = InventoryAccessFactoryTest.class.getClassLoader();
    private static final ServerRuntime SPIGOT_1_21_4 = new ServerRuntime("v1_21_R3", MappingType.SPIGOT, LOADER);

    @Test
    void providerSupportsVersionWithoutNames() throws Exception {
        // A dev sandbox for a version that is not in the name tables yet
        MinecraftVersion version = MinecraftVersion.parse(TestProvider.WITHOUT_NAMES);
        assertSame(TestProvider.ACCESS, InventoryAccessFactory.create(version, SPIGOT_1_21_4));
    }

    @Test
    void providerComesBeforeReflection() throws Exception {
        assertSame(TestProvider.ACCESS,
                InventoryAccessFactory.create(MinecraftVersion.parse(TestProvider.WITH_NAMES), SPIGOT_1_21_4));
    }

    @Test
    void unsupportedWithoutProviderOrNames() {
        MinecraftVersion version = MinecraftVersion.parse("1.11.2");
        assertThrows(UnsupportedVersionException.class, () -> InventoryAccessFactory.create(version, SPIGOT_1_21_4));
    }

    @Test
    void incompatibleWithoutServerClasses() {
        // 1.21.4 is supported, but this test classpath has no CraftBukkit/NMS classes
        MinecraftVersion version = MinecraftVersion.parse("1.21.4");
        assertThrows(IncompatibleServerException.class, () -> InventoryAccessFactory.create(version, SPIGOT_1_21_4));
    }

    /** Registered in {@code src/test/resources/META-INF/services}. */
    public static final class TestProvider implements InventoryAccessProvider {
        /** In no name table */
        static final String WITHOUT_NAMES = "1.0";
        /** Also supported by the reflection provider */
        static final String WITH_NAMES = "1.20.4";
        static final InventoryAccess ACCESS = new InventoryAccess() {
            @Override
            public @NotNull InventoryStorage createEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @NotNull InventoryStorage capture(@NotNull Player player) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void apply(@NotNull Player player, @NotNull InventoryStorage inventory) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @NotNull ItemStack getItem(@NotNull InventoryStorage inventory, int slot) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setItem(@NotNull InventoryStorage inventory, int slot, @NotNull ItemStack item) {
                throw new UnsupportedOperationException();
            }
        };

        @Override
        public boolean supports(@NotNull MinecraftVersion version) {
            return version.equals(MinecraftVersion.parse(WITHOUT_NAMES))
                    || version.equals(MinecraftVersion.parse(WITH_NAMES));
        }

        @Override
        public @NotNull InventoryAccess create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime) {
            return ACCESS;
        }
    }
}
