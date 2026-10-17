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

import com.sugar_tree.inventoryshare.nms.legacy.LegacyMembers;
import com.sugar_tree.inventoryshare.nms.legacy.LegacyNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Legacy NMS mapping (1.12 - 1.21.4) against Spigot runtime and remapped-mojang jars")
class LegacyMappingTest {

    private static final String FIRST_MODERN = "1.21.5";

    static Stream<String> legacyVersions() {
        return SpigotJars.versionsBelow(FIRST_MODERN);
    }

    static Stream<SpigotJars.Jar> legacyJars() {
        return SpigotJars.jarsBelow(FIRST_MODERN);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("legacyJars")
    void mappingResolves(SpigotJars.Jar jar) throws Exception {
        LegacyNames row = legacyNames(jar.mc);

        try (URLClassLoader loader = SpigotJars.open(jar)) {
            // The runtime is detected from the jar exactly as on a server
            ServerRuntime runtime = ServerRuntime.detect(SpigotJars.craftServerClass(jar), loader);
            assertEquals(jar.expectedNamespace(), runtime.getNamespace(), "namespace");
            assertEquals(row.getCraftbukkitPackage(), runtime.getCraftbukkitPackage(), "CraftBukkit package of the row");

            LegacyNames names = row.forRuntime(runtime);
            LegacyMembers members = LegacyMembers.resolve(names, loader);
            Class<?> nonNullList = Class.forName(names.getNonNullListClass(), false, loader);
            Class<?> itemStack = members.getItemStack();

            // CraftPlayer#getHandle reaches the class declaring the inventory member, which yields PlayerInventory
            Member inventory = members.getInventory();
            assertTrue(inventory.getDeclaringClass().isAssignableFrom(members.getGetHandle().getReturnType()),
                    "getHandle() must return a subtype of " + inventory.getDeclaringClass().getName());
            Class<?> inventoryType = inventory instanceof Field
                    ? ((Field) inventory).getType()
                    : ((Method) inventory).getReturnType();
            assertEquals(members.getPlayerInventory(), inventoryType);

            assertItemList(members.getItems(), nonNullList, itemStack);
            assertItemList(members.getArmor(), nonNullList, itemStack);
            assertItemList(members.getExtraSlots(), nonNullList, itemStack);

            // contents: List<NonNullList<ItemStack>>
            Field contents = members.getContents();
            assertEquals(List.class, contents.getType());
            Type inner = ((ParameterizedType) contents.getGenericType()).getActualTypeArguments()[0];
            assertEquals(nonNullList, ((ParameterizedType) inner).getRawType());

            Field emptyItem = members.getEmptyItem();
            assertTrue(Modifier.isStatic(emptyItem.getModifiers()), "empty item must be static");
            assertEquals(itemStack, emptyItem.getType());

            Method createItemList = members.getCreateItemList();
            assertTrue(Modifier.isStatic(createItemList.getModifiers()), "item list factory must be static");
            assertEquals(nonNullList, createItemList.getReturnType());

            Class<?> bukkitItemStack = Class.forName("org.bukkit.inventory.ItemStack", false, loader);
            assertEquals(bukkitItemStack, members.getAsBukkitCopy().getReturnType());
            assertEquals(itemStack, members.getAsNMSCopy().getReturnType());
        }
    }

    @Test
    @DisplayName("every legacy row is checked by at least one listed version")
    void everyLegacyRowIsCovered() {
        Set<NmsMapping> covered = legacyVersions()
                .map(mc -> NmsMapping.find(ServerVersion.parse(mc)))
                .collect(Collectors.toSet());
        for (NmsMapping mapping : NmsMapping.values()) {
            if (mapping.getLayout() instanceof LegacyNames) {
                assertTrue(covered.contains(mapping), mapping + " has no Spigot version in InventoryShare-nms-test/build.gradle");
            }
        }
    }

    private static LegacyNames legacyNames(String mc) {
        NmsMapping mapping = NmsMapping.find(ServerVersion.parse(mc));
        assertNotNull(mapping, "no NmsMapping row for " + mc);
        assertTrue(mapping.getLayout() instanceof LegacyNames, mapping + " is not a legacy row");
        return (LegacyNames) mapping.getLayout();
    }

    private static void assertItemList(Field field, Class<?> nonNullList, Class<?> itemStack) {
        assertEquals(nonNullList, field.getType(), field.getName());
        Type element = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        assertEquals(itemStack, element, field.getName());
    }
}
