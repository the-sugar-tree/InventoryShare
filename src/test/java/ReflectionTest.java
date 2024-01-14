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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

public class ReflectionTest {
    public static void main(String[] args) throws ClassNotFoundException {
        new Car(new CarName("트럭"), 3, "ㅋ", "ㅋㅋ");

        for (Field field : Class.forName("ReflectionTest$Car").getDeclaredFields()) {
            System.out.println(field.getType().getName());
            if (field.getType().equals(Class.forName("ReflectionTest$CarName"))) {
                System.out.println(field.getName());
            }
            if (field.getType().equals(String.class)) {
                System.out.println(field.getName());
            }
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class Car {
        private final CarName name;
        private final int number_of_wheel;
        private final String subname;
        private final String subname2;
    }

    @Getter
    @RequiredArgsConstructor
    static class CarName {
        private final String name;
    }

}
