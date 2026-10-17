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

import com.sugar_tree.inventoryshare.nms.modern.ModernMembers;
import com.sugar_tree.inventoryshare.nms.modern.ModernNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Modern NMS mapping (1.21.5+) against Spigot runtime and remapped-mojang jars")
class ModernMappingTest {

    private static final String FIRST_MODERN = "1.21.5";

    /** Expected EquipmentSlot (Mojang name, ordinal) for inventory slots 36 - 40 */
    private static final String[] SLOT_NAMES = {"FEET", "LEGS", "CHEST", "HEAD", "OFFHAND"};
    private static final int[] SLOT_ORDINALS = {2, 3, 4, 5, 1};

    static Stream<String> modernVersions() {
        return SpigotJars.versionsFrom(FIRST_MODERN);
    }

    static Stream<SpigotJars.Jar> modernJars() {
        return SpigotJars.jarsFrom(FIRST_MODERN);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modernJars")
    void mappingResolves(SpigotJars.Jar jar) throws Exception {
        ModernNames row = modernNames(jar.mc);

        try (URLClassLoader loader = SpigotJars.open(jar)) {
            // The runtime is detected from the jar exactly as on a server
            ServerRuntime runtime = ServerRuntime.detect(SpigotJars.craftServerClass(jar), loader);
            assertEquals(jar.expectedNamespace(), runtime.getNamespace(), "namespace");
            assertEquals(row.getCraftbukkitPackage(), runtime.getCraftbukkitPackage(), "CraftBukkit package of the row");

            ModernNames names = row.forRuntime(runtime);
            ModernMembers members = ModernMembers.resolve(names, loader);
            Class<?> itemStack = members.getItemStack();

            // CraftPlayer#getHandle reaches the class declaring getInventory(), which returns the inventory
            Method getInventory = members.getGetInventory();
            assertTrue(getInventory.getDeclaringClass().isAssignableFrom(members.getGetHandle().getReturnType()),
                    "getHandle() must return a subtype of " + getInventory.getDeclaringClass().getName());
            assertEquals(members.getInventory(), getInventory.getReturnType());

            // Inventory#items: NonNullList<ItemStack>
            Field items = members.getItems();
            assertEquals(members.getNonNullList(), items.getType());
            assertEquals(itemStack, typeArgument(items.getGenericType(), 0));

            // Inventory#equipment: EntityEquipment, EntityEquipment#items: EnumMap<EquipmentSlot, ItemStack>
            assertEquals(members.getEntityEquipment(), members.getEquipment().getType());
            Field equipmentItems = members.getEquipmentItems();
            assertEquals(EnumMap.class, equipmentItems.getType());
            assertEquals(members.getEquipmentSlot(), typeArgument(equipmentItems.getGenericType(), 0));
            assertEquals(itemStack, typeArgument(equipmentItems.getGenericType(), 1));

            // Inventory#EQUIPMENT_SLOT_MAPPING: static Int2ObjectMap<EquipmentSlot>
            Field slotMapping = members.getSlotMapping();
            assertTrue(Modifier.isStatic(slotMapping.getModifiers()), "slot mapping must be static");
            assertTrue(Map.class.isAssignableFrom(slotMapping.getType()), "slot mapping must be a Map");
            assertEquals(members.getEquipmentSlot(), typeArgument(slotMapping.getGenericType(), 0));

            Field emptyItem = members.getEmptyItem();
            assertTrue(Modifier.isStatic(emptyItem.getModifiers()), "empty item must be static");
            assertEquals(itemStack, emptyItem.getType());

            Method createItemList = members.getCreateItemList();
            assertTrue(Modifier.isStatic(createItemList.getModifiers()), "item list factory must be static");
            assertEquals(members.getNonNullList(), createItemList.getReturnType());

            Class<?> bukkitItemStack = Class.forName("org.bukkit.inventory.ItemStack", false, loader);
            assertEquals(bukkitItemStack, members.getAsBukkitCopy().getReturnType());
            assertEquals(itemStack, members.getAsNMSCopy().getReturnType());

            assertSlotMapping(names, members);
        }
    }

    @Test
    @DisplayName("every modern row is checked by at least one listed version")
    void everyModernRowIsCovered() {
        Set<NmsMapping> covered = modernVersions()
                .map(mc -> NmsMapping.find(ServerVersion.parse(mc)))
                .collect(Collectors.toSet());
        for (NmsMapping mapping : NmsMapping.values()) {
            if (mapping.getLayout() instanceof ModernNames) {
                assertTrue(covered.contains(mapping), mapping + " has no Spigot version in InventoryShare-nms-test/build.gradle");
            }
        }
    }

    /**
     * Slots 36 - 40 must map to feet, legs, chest, head, offhand. Spigot obfuscates the enum constants
     * ({@code a}..{@code h}), so they are compared by ordinal, and by name where names are available.
     */
    private static void assertSlotMapping(ModernNames names, ModernMembers members) throws IllegalAccessException {
        Map<?, ?> slotMapping = (Map<?, ?>) members.getSlotMapping().get(null);
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            int slot = InventoryAccess.ARMOR_START + i;
            Enum<?> equipmentSlot = (Enum<?>) slotMapping.get(slot);
            assertNotNull(equipmentSlot, "no equipment slot for inventory slot " + slot);
            assertEquals(SLOT_ORDINALS[i], equipmentSlot.ordinal(), "inventory slot " + slot);
            if (names.getNamespace() == Namespace.MOJANG) {
                assertEquals(SLOT_NAMES[i], equipmentSlot.name(), "inventory slot " + slot);
            }
        }
    }

    private static ModernNames modernNames(String mc) {
        NmsMapping mapping = NmsMapping.find(ServerVersion.parse(mc));
        assertNotNull(mapping, "no NmsMapping row for " + mc);
        assertTrue(mapping.getLayout() instanceof ModernNames, mapping + " is not a modern row");
        return (ModernNames) mapping.getLayout();
    }

    private static Type typeArgument(Type type, int index) {
        return ((ParameterizedType) type).getActualTypeArguments()[index];
    }
}
