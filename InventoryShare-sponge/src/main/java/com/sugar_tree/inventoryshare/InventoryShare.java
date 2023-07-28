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

package com.sugar_tree.inventoryshare;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("inventoryshare")
public class InventoryShare {
    public static Logger logger;

    @Inject
    private Logger rawLogger;

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        logger = rawLogger;
        logger.info("Successfully running Plugin!");

//        try {
//            logger.info("Data from net.minecraft.world.item.ItemStack");
//            Field[] fields = Class.forName("net.minecraft.world.item.ItemStack").getDeclaredFields();
//            for (int i = 0; i < fields.length; i++) {
//                logger.info("#" + i + " " + fields[i].getName());
//            }
//        } catch (Exception e) {
//            logger.info(e);
//        }
//
//        try {
//            logger.info("Data from net.minecraft.core.NonNullList");
//            Method[] methods = Class.forName("net.minecraft.core.NonNullList").getDeclaredMethods();
//            for (int i = 0; i < methods.length; i++) {
//                logger.info("#" + i + " " + methods[i].getName());
//            }
//        } catch (Exception e) {
//            logger.info(e);
//        }
//
//        try {
//            logger.info("Data from org.spongepowered.common.entity.player");
//            Class.forName("org.spongepowered.common.entity.player");
//        } catch (Exception e) {
//            logger.info(e);
//        }
    }

//    @Listener
//    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
//        Parameter.Value<ServerPlayer> playerParameter = Parameter.player().key("player").build();
//        Command.Parameterized command = Command.builder()
//                .executor((CommandContext context) -> {
//                    ServerPlayer player = context.requireOne(playerParameter);
//                    logger.info(player.inventory().getClass().getName());
//                    Field[] fields = player.inventory().getClass().getFields();
//                    for (int i = 0; i < fields.length; i++) {
//                        logger.info("#" + i + " " + fields[i].getName());
//                    }
//                    return CommandResult.success();
//                })
//                .addParameters(playerParameter)
//                .build();
//        event.register(this.container, command, "applytest");
//    }

    @Inject
    PluginContainer container;

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) throws ClassNotFoundException {
        Parameter.Value<ServerPlayer> playerParameter = Parameter.player().key("player").build();
        Command.Parameterized command = Command.builder()
                .executor((CommandContext context) -> {
                    ServerPlayer player = context.requireOne(playerParameter);
                    try {
                        Inventory.task(player);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return CommandResult.success();
                })
                .addParameters(playerParameter)
                .build();
        event.register(this.container, command, "applytest");
        Class<?> invClass = Class.forName("net.minecraft.world.entity.player.Inventory");
        Command.Parameterized command1 = Command.builder()
                .executor((CommandContext context) -> {
                    ServerPlayer player = context.requireOne(playerParameter);
                    try {
                        logger.info(toString(invClass.getField("items").get(player.inventory())));
                        Inventory.updateInventory(player);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                    return CommandResult.success();
                })
                .addParameters(playerParameter)
                .build();
        event.register(this.container, command1, "debug");
    }

    private static String toString(Object o) {
        return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }
}
