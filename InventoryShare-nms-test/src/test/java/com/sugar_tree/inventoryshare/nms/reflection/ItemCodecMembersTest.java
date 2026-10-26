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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.ItemCodecFactory;
import com.sugar_tree.inventoryshare.nms.reflection.item.ItemCodecMembers;
import com.sugar_tree.inventoryshare.nms.reflection.item.ItemCodecNames;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Item codec names (1.20.5+) resolved against Spigot runtime and remapped-mojang jars")
class ItemCodecMembersTest {

    static Stream<SpigotJars.Jar> itemCodecJars() {
        return SpigotJars.jarsFrom(ItemCodecFactory.FIRST_VERSION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("itemCodecJars")
    void membersResolve(SpigotJars.Jar jar) throws Exception {
        try (URLClassLoader loader = SpigotJars.open(jar)) {
            ServerRuntime runtime = ServerRuntime.detect(SpigotJars.craftServerClass(jar), loader);
            assertEquals(jar.expectedMappingType(), runtime.getMappingType(), "mapping type");
            ItemCodecNames names = NmsNameFactory.itemCodec(jar.version(), runtime);
            ItemCodecMembers members = ItemCodecMembers.resolve(names, loader);
            Class<?> itemStack = members.getItemStack();

            // ItemStack.CODEC: static Codec<ItemStack>
            Field codec = members.getCodec();
            assertStatic(codec);
            assertEquals("com.mojang.serialization.Codec", codec.getType().getName());
            assertEquals(itemStack, ((ParameterizedType) codec.getGenericType()).getActualTypeArguments()[0]);

            // NbtOps.INSTANCE: static NbtOps
            Field nbtOps = members.getNbtOps();
            assertStatic(nbtOps);
            assertEquals(nbtOps.getDeclaringClass(), nbtOps.getType());

            // CraftRegistry.getMinecraftRegistry() returns the provider that createSerializationContext belongs to
            Method createSerializationContext = members.getCreateSerializationContext();
            assertTrue(createSerializationContext.getDeclaringClass()
                            .isAssignableFrom(members.getGetMinecraftRegistry().getReturnType()),
                    "getMinecraftRegistry() must return a " + createSerializationContext.getDeclaringClass().getName());
            assertEquals("net.minecraft.resources.RegistryOps", createSerializationContext.getReturnType().getName());

            // TagParser: static CompoundTag parse(String)
            Method parseTag = members.getParseTag();
            assertStatic(parseTag);
            assertTrue(Arrays.asList("net.minecraft.nbt.CompoundTag", "net.minecraft.nbt.NBTTagCompound")
                    .contains(parseTag.getReturnType().getName()), "parseTag returns " + parseTag.getReturnType());

            Method getDataFixer = members.getGetDataFixer();
            assertStatic(getDataFixer);
            assertEquals("com.mojang.datafixers.DataFixer", getDataFixer.getReturnType().getName());

            assertEquals(itemStack, members.getAsNMSCopy().getReturnType());
            assertEquals(itemStack, members.getAsCraftMirror().getParameterTypes()[0]);
        }
    }

    /**
     * Several static {@code Codec<ItemStack>} fields exist ({@code CODEC}, {@code OPTIONAL_CODEC}, ...), so the type
     * cannot tell Spigot's {@code CODEC} apart. Remapping keeps the declaration order, so the field Spigot's name
     * points to must be declared where the remapped-mojang jar of the same version declares {@code CODEC}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("spigotNamedVersions")
    void spigotCodecIsMojangCodec(String mc) throws Exception {
        SpigotJars.Jar spigot = SpigotJars.jars().filter(jar -> jar.mc.equals(mc) && !jar.mojangRemapped)
                .findFirst().orElseThrow(IllegalStateException::new);
        SpigotJars.Jar mojang = SpigotJars.jars().filter(jar -> jar.mc.equals(mc) && jar.mojangRemapped)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(mojang != null, "no remapped-mojang jar listed for " + mc);

        int spigotIndex;
        try (URLClassLoader loader = SpigotJars.open(spigot)) {
            ServerRuntime runtime = ServerRuntime.detect(SpigotJars.craftServerClass(spigot), loader);
            ItemCodecNames names = NmsNameFactory.itemCodec(spigot.version(), runtime);
            spigotIndex = fieldNames(Class.forName(names.getItemStackClass(), false, loader)).indexOf(names.getCodec());
        }
        try (URLClassLoader loader = SpigotJars.open(mojang)) {
            List<String> mojangFields = fieldNames(Class.forName("net.minecraft.world.item.ItemStack", false, loader));
            assertEquals(mojangFields.indexOf("CODEC"), spigotIndex, "declared fields (Mojang): " + mojangFields);
        }
    }

    @Test
    @DisplayName("every item codec row is checked by at least one listed version")
    void everyRowIsCovered() {
        Set<ItemCodecNameTable> covered = SpigotJars.versionsFrom(ItemCodecFactory.FIRST_VERSION)
                .map(mc -> ItemCodecNameTable.find(MinecraftVersion.parse(mc)))
                .collect(Collectors.toSet());
        for (ItemCodecNameTable row : ItemCodecNameTable.values()) {
            assertTrue(covered.contains(row), row + " has no Spigot version in InventoryShare-nms-test/build.gradle");
        }
    }

    /** Versions whose Spigot jar uses Spigot's names (1.20.5 - 1.21.11) */
    static Stream<String> spigotNamedVersions() {
        return SpigotJars.versionsFrom(ItemCodecFactory.FIRST_VERSION)
                .filter(mc -> MinecraftVersion.parse(mc).isOlderThan("26.1"));
    }

    /** Names of the declared fields, in declaration order */
    private static List<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
    }

    private static void assertStatic(java.lang.reflect.Member member) {
        assertTrue(Modifier.isStatic(member.getModifiers()), member.getName() + " must be static");
    }
}
