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

package com.sugar_tree.inventoryshare.nms.legacy;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.Namespace;
import com.sugar_tree.inventoryshare.nms.NmsLayout;
import com.sugar_tree.inventoryshare.nms.ServerRuntime;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NMS member names for the {@code items / armor / extraSlots / contents} inventory model (1.12 - 1.21.4).
 */
@Getter
public final class LegacyNames implements NmsLayout {

    private final Namespace namespace;
    /**
     * {@code vX_Y_RZ} package of CraftBukkit (and of NMS before 1.17). In the mapping table it is the package of
     * Spigot's release; on a server it is replaced by the server's own one, {@code null} if not relocated.
     */
    private final @Nullable String craftbukkitPackage;
    /** 1.17+: NMS packages are flattened and the player inventory is reached through a method */
    private final boolean flattened;
    /** inventory field (pre-1.17) or method (1.17+) on the player */
    private final String inventory;
    private final String items;
    private final String armor;
    private final String extraSlots;
    private final String contents;
    private final String emptyItem;
    private final String createItemList;

    private LegacyNames(Namespace namespace, @Nullable String craftbukkitPackage, boolean flattened, String inventory,
                        String items, String armor, String extraSlots, String contents, String emptyItem,
                        String createItemList) {
        this.namespace = namespace;
        this.craftbukkitPackage = craftbukkitPackage;
        this.flattened = flattened;
        this.inventory = inventory;
        this.items = items;
        this.armor = armor;
        this.extraSlots = extraSlots;
        this.contents = contents;
        this.emptyItem = emptyItem;
        this.createItemList = createItemList;
    }

    /** Pre-1.17: {@code net.minecraft.server.vX_Y_RZ} packages, inventory is a field. */
    public static LegacyNames legacy(String craftbukkitPackage, String inventory, String items, String armor,
                                     String extraSlots, String contents, String emptyItem) {
        return new LegacyNames(Namespace.SPIGOT, craftbukkitPackage, false, inventory, items, armor, extraSlots,
                contents, emptyItem, "a");
    }

    /** 1.17 - 1.21.4: flattened NMS packages, inventory is a method. Spigot-obfuscated member names. */
    public static LegacyNames flattened(String craftbukkitPackage, String inventory, String items, String armor,
                                        String extraSlots, String contents, String emptyItem) {
        return new LegacyNames(Namespace.SPIGOT, craftbukkitPackage, true, inventory, items, armor, extraSlots,
                contents, emptyItem, "a");
    }

    /**
     * 1.17 - 1.21.4 on a server running with Mojang's names (Paper 1.20.5+, Paper's Mojang-mapped 1.20.4).
     * The names did not change within this range.
     */
    public static LegacyNames mojang(@Nullable String craftbukkitPackage) {
        return new LegacyNames(Namespace.MOJANG, craftbukkitPackage, true, "getInventory", "items", "armor",
                "offhand", "compartments", "EMPTY", "withSize");
    }

    @Override
    public @NotNull LegacyNames forRuntime(@NotNull ServerRuntime runtime) {
        if (runtime.getNamespace() == Namespace.MOJANG) {
            if (!flattened) {
                throw new IllegalStateException("No Mojang names for the pre-1.17 inventory model");
            }
            return mojang(runtime.getCraftbukkitPackage());
        }
        return new LegacyNames(namespace, runtime.getCraftbukkitPackage(), flattened, inventory, items, armor,
                extraSlots, contents, emptyItem, createItemList);
    }

    public String getCraftPlayerClass() {
        return craftbukkit("entity.CraftPlayer");
    }

    public String getCraftItemStackClass() {
        return craftbukkit("inventory.CraftItemStack");
    }

    /** Class declaring the {@link #getInventory() inventory} member. */
    public String getInventoryOwnerClass() {
        if (!flattened) return nms("EntityHuman");
        return namespace == Namespace.MOJANG
                ? "net.minecraft.server.level.ServerPlayer"
                : "net.minecraft.server.level.EntityPlayer";
    }

    public String getPlayerInventoryClass() {
        if (!flattened) return nms("PlayerInventory");
        return namespace == Namespace.MOJANG
                ? "net.minecraft.world.entity.player.Inventory"
                : "net.minecraft.world.entity.player.PlayerInventory";
    }

    public String getItemStackClass() {
        return flattened ? "net.minecraft.world.item.ItemStack" : nms("ItemStack");
    }

    public String getNonNullListClass() {
        return flattened ? "net.minecraft.core.NonNullList" : nms("NonNullList");
    }

    private String craftbukkit(String name) {
        return craftbukkitPackage == null
                ? "org.bukkit.craftbukkit." + name
                : "org.bukkit.craftbukkit." + craftbukkitPackage + "." + name;
    }

    private String nms(String simpleName) {
        if (craftbukkitPackage == null) {
            throw new IllegalStateException("Pre-1.17 NMS classes need a versioned package");
        }
        return "net.minecraft.server." + craftbukkitPackage + "." + simpleName;
    }

    @Override
    public @NotNull InventoryAccess createAccess(@NotNull ServerRuntime runtime) throws ReflectiveOperationException {
        return new LegacyInventoryAccess(LegacyMembers.resolve(forRuntime(runtime), runtime.getClassLoader()));
    }
}
