/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
