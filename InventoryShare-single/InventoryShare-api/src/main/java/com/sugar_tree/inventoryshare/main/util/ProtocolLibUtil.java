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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.main.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashSet;
import java.util.Set;

import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;

public class ProtocolLibUtil {
    static Set<Player> breakingBlock = new HashSet<>();
    public static void ProtocolLib() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (breakingBlock.contains(event.getPlayer())) {
                    Object slot = event.getPacket().getModifier().read(2);
                    if ((event.getPlayer().getInventory().getHeldItemSlot() + 36) != ((int) slot)) {
                        return;
                    }
                    ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
                    if (handItem.hasItemMeta() && handItem.getItemMeta() instanceof Damageable && ((Damageable) handItem.getItemMeta()).getDamage() > 0) {
                        event.setCancelled(true);
                    }
                }
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object Status = event.getPacket().getModifier().read(2);
                try {
                    String value = Status.toString();
                    switch (value) {
                        case "ABORT_DESTROY_BLOCK":
                        case "STOP_DESTROY_BLOCK":
                            breakingBlock.remove(event.getPlayer());
                            event.getPlayer().updateInventory();
                            break;
                        case "START_DESTROY_BLOCK":
                            breakingBlock.add(event.getPlayer());
                            break;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
    }
}
