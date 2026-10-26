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

package com.sugar_tree.inventoryshare.nms.reflection.item;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.sugar_tree.inventoryshare.nms.ItemCodec;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.get;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.invoke;

/**
 * {@link ItemCodec} with Minecraft's {@code ItemStack.CODEC}: an item is encoded to NBT with ops that can look up the
 * server's registries, and each part of the NBT is stored as a YAML value (id, count) or as SNBT (components). Decoding
 * builds the SNBT of the whole item, parses it, upgrades it with the data fixer if it is older, and decodes it.
 *
 * <p>Uses DataFixerUpper's types directly; the class is loaded only on 1.20.5+, where the server provides them.
 */
@SuppressWarnings("unchecked")
public final class VanillaItemCodec implements ItemCodec {

    private static final String ID = "id";
    private static final String COUNT = "count";
    private static final String COMPONENTS = "components";
    /** Paper's layout version; Paper reads a map with it through its own deserializer */
    private static final String SCHEMA_VERSION = "schema_version";
    private static final int SCHEMA = 1;
    private static final String AIR = "minecraft:air";
    /** {@code References.ITEM_STACK}: the data fixer looks types up by name */
    private static final DSL.TypeReference ITEM_STACK = () -> "item_stack";

    private final ItemCodecMembers members;
    private final int dataVersion;
    private final Codec<Object> codec;
    private final DynamicOps<Object> nbtOps;
    private final DynamicOps<Object> registryOps;
    private final DataFixer dataFixer;

    public VanillaItemCodec(@NotNull ItemCodecMembers members, int dataVersion) {
        this.members = members;
        this.dataVersion = dataVersion;
        this.codec = (Codec<Object>) get(members.getCodec(), null);
        this.nbtOps = (DynamicOps<Object>) get(members.getNbtOps(), null);
        Object registries = invoke(members.getGetMinecraftRegistry(), null);
        this.registryOps = (DynamicOps<Object>) invoke(members.getCreateSerializationContext(), registries, nbtOps);
        this.dataFixer = (DataFixer) invoke(members.getGetDataFixer(), null);
        verify();
    }

    /** Converts an item both ways, so a broken member fails while the plugin is starting */
    private void verify() {
        ItemStack item = decode(encode(new ItemStack(Material.STONE, 2)));
        if (item.getType() != Material.STONE || item.getAmount() != 2) {
            throw new IllegalStateException("The item codec changed a test item into " + item);
        }
    }

    @Override
    public @NotNull Map<String, Object> encode(@NotNull ItemStack item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(DATA_VERSION, dataVersion);
        if (item.getType() == Material.AIR || item.getAmount() <= 0) {
            data.put(ID, AIR);
            data.put(SCHEMA_VERSION, SCHEMA);
            return data;
        }
        Object tag = codec.encodeStart(registryOps, invoke(members.getAsNMSCopy(), null, item))
                .getOrThrow(message -> new IllegalStateException("Could not encode " + item + ": " + message));
        // The compound's order is not fixed; id, count and components come first, as in Paper's layout
        Map<String, Object> others = new LinkedHashMap<>();
        for (Pair<Object, Object> entry : entries(tag)) {
            String key = string(entry.getFirst());
            Object value = entry.getSecond();
            if (key.equals(ID)) {
                data.put(ID, string(value));
            } else if (key.equals(COUNT)) {
                others.put(COUNT, nbtOps.getNumberValue(value).getOrThrow(IllegalStateException::new).intValue());
            } else if (key.equals(COMPONENTS)) {
                Map<String, Object> components = new TreeMap<>();
                for (Pair<Object, Object> component : entries(value)) {
                    components.put(string(component.getFirst()), component.getSecond().toString());
                }
                others.put(COMPONENTS, components);
            } else {
                // Not written by any version so far; kept as SNBT and put back as is
                others.put(key, value.toString());
            }
        }
        if (others.containsKey(COUNT)) data.put(COUNT, others.remove(COUNT));
        if (others.containsKey(COMPONENTS)) data.put(COMPONENTS, others.remove(COMPONENTS));
        data.putAll(others);
        data.put(SCHEMA_VERSION, SCHEMA);
        return data;
    }

    @Override
    public @NotNull ItemStack decode(@NotNull Map<?, ?> data) {
        Object version = data.get(DATA_VERSION);
        if (!(version instanceof Number)) {
            throw new IllegalArgumentException("No " + DATA_VERSION + ": " + data);
        }
        int itemVersion = ((Number) version).intValue();
        if (itemVersion > dataVersion) {
            throw new IllegalArgumentException("Saved by a newer Minecraft version (data version " + itemVersion
                    + " > " + dataVersion + ")");
        }
        Object id = data.get(ID);
        if (id == null) {
            throw new IllegalArgumentException("No " + ID + ": " + data);
        }
        if (id.toString().equals(AIR)) {
            return new ItemStack(Material.AIR);
        }

        Object tag;
        try {
            tag = invoke(members.getParseTag(), null, snbt(data));
        } catch (IllegalStateException e) {
            // The parser's CommandSyntaxException, wrapped by Reflections.invoke
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalArgumentException("Invalid SNBT: " + cause.getMessage(), cause);
        }
        if (itemVersion < dataVersion) {
            tag = dataFixer.update(ITEM_STACK, new Dynamic<>(nbtOps, tag), itemVersion, dataVersion).getValue();
        }
        Object item = codec.parse(registryOps, tag)
                .getOrThrow(message -> new IllegalArgumentException("Could not decode " + id + ": " + message));
        return (ItemStack) invoke(members.getCraftMirror(), null, item);
    }

    /**
     * @return {@code {id:"…",count:…,components:{"…":…}}}, with every other key except the data and schema versions
     *         put back as written
     */
    private static String snbt(Map<?, ?> data) {
        StringBuilder snbt = new StringBuilder("{");
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (key.equals(DATA_VERSION) || key.equals(SCHEMA_VERSION) || value == null) continue;
            if (snbt.length() > 1) snbt.append(',');
            quote(snbt, key).append(':');
            if (key.equals(ID)) {
                quote(snbt, value.toString());
            } else if (key.equals(COMPONENTS) && value instanceof Map) {
                snbt.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> component : ((Map<?, ?>) value).entrySet()) {
                    if (!first) snbt.append(',');
                    first = false;
                    quote(snbt, String.valueOf(component.getKey())).append(':').append(component.getValue());
                }
                snbt.append('}');
            } else {
                snbt.append(value);
            }
        }
        return snbt.append('}').toString();
    }

    private static StringBuilder quote(StringBuilder snbt, String text) {
        snbt.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') snbt.append('\\');
            snbt.append(c);
        }
        return snbt.append('"');
    }

    private Iterable<Pair<Object, Object>> entries(Object compound) {
        return () -> nbtOps.getMapValues(compound).getOrThrow(IllegalStateException::new).iterator();
    }

    private String string(Object tag) {
        return nbtOps.getStringValue(tag).getOrThrow(IllegalStateException::new);
    }
}
