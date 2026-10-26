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

import com.sugar_tree.inventoryshare.nms.reflection.Reflections.FieldWrite;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionsTest {

    private static final Field FIRST = field("first");
    private static final Field SECOND = field("second");
    private static final Field NUMBER = field("number");

    @Test
    void setAllWritesEveryField() {
        Target a = new Target();
        Target b = new Target();
        Reflections.setAll(FieldWrite.of(FIRST, a, "a1"), FieldWrite.of(SECOND, a, "a2"), FieldWrite.of(FIRST, b, "b1"));
        assertEquals("a1", a.first);
        assertEquals("a2", a.second);
        assertEquals("b1", b.first);
    }

    @Test
    void anyMethodTakesTheFirstNameThatExists() throws NoSuchMethodException {
        assertEquals("valueOf", Reflections.anyMethod(String.class, Object.class, "missing", "valueOf").getName());
        assertThrows(NoSuchMethodException.class, () -> Reflections.anyMethod(String.class, Object.class, "missing"));
    }

    @Test
    void setAllRestoresWrittenFieldsWhenOneFails() {
        Target a = new Target();
        Target b = new Target();
        // A String does not fit the Integer field, so the third write fails
        assertThrows(IllegalArgumentException.class, () -> Reflections.setAll(
                FieldWrite.of(FIRST, a, "a1"), FieldWrite.of(FIRST, b, "b1"), FieldWrite.of(NUMBER, a, "not a number"),
                FieldWrite.of(SECOND, a, "a2")));
        assertEquals("first", a.first);
        assertEquals("second", a.second);
        assertEquals(1, a.number);
        assertEquals("first", b.first);
    }

    private static Field field(String name) {
        try {
            return Reflections.accessible(Target.class.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private static final class Target {
        private String first = "first";
        private String second = "second";
        private Integer number = 1;
    }
}
