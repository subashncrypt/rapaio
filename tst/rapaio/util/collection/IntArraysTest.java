/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.util.collection;

import static org.junit.jupiter.api.Assertions.*;

import static rapaio.util.collection.IntArrays.*;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rapaio.util.IntComparator;
import rapaio.util.IntComparators;
import rapaio.util.IntIterator;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/11/19.
 */
public class IntArraysTest {

    private Random random;

    @BeforeEach
    void beforeEach() {
        random = new Random(1234);
    }

    @Test
    void buildersTest() {
        assertArrayEquals(new int[] {10, 10, 10}, IntArrays.newFill(3, 10));
        assertArrayEquals(new int[] {10, 11, 12}, IntArrays.newSeq(10, 13));
        assertArrayEquals(new int[] {4, 9, 16}, IntArrays.newFrom(new int[] {1, 2, 3, 4, 5}, 1, 4, x -> x * x));
        assertArrayEquals(new int[] {3, 5}, IntArrays.newCopy(new int[] {1, 3, 5, 7}, 1, 2));
    }

    private void testEqualArrays(int[] actual, int... expected) {
        Assertions.assertArrayEquals(expected, actual);
    }


    @Test
    void testIterator() {
        int[] array = new int[1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt(100);
        }
        IntIterator it1 = IntArrays.iterator(array, 0, 10);
        for (int i = 0; i < 10; i++) {
            assertTrue(it1.hasNext());
            assertEquals(array[i], it1.nextInt());
        }
        assertThrows(NoSuchElementException.class, it1::nextInt);

        var it2 = IntArrays.iterator(array, 0, 100);
        for (int i = 0; i < 100; i++) {
            assertTrue(it2.hasNext());
            assertEquals(array[i], it2.nextInt());
        }
        assertThrows(NoSuchElementException.class, it2::nextInt);
    }

    @Test
    void testMinus() {
        int[] a = IntArrays.newSeq(0, 100);
        int[] b = IntArrays.copy(a);

        IntArrays.sub(a, 0, b, 0, 100);
        for (int j : a) {
            assertEquals(0, j);
        }
    }

    @Test
    void testDot() {
        var a = IntArrays.newSeq(0, 100);
        var b = IntArrays.copy(a);

        int result = IntArrays.prodsum(a, 0, b, 0, 100);
        for (int i = 0; i < a.length; i++) {
            result -= i * i;
        }
        assertEquals(0, result);

        result = IntArrays.prod(a, 1, 10);
        for (int i = 1; i < 11; i++) {
            result /= a[i];
        }
        assertEquals(1, result);
    }

    @Test
    void testCapacity() {
        var array1 = newSeq(0, 100);

        // new copy preserving 10
        var array2 = forceCapacity(array1, 10, 10);
        assertTrue(IntArrays.equals(array1, 0, array2, 0, 10));
        assertEquals(10, array2.length);

        // new copy preserving 80
        var array3 = forceCapacity(array1, 120, 80);
        assertTrue(IntArrays.equals(array1, 0, array3, 0, 80));
        assertEquals(120, array3.length);

        // leave array untouched
        var array4 = ensureCapacity(array1, 10);
        assertTrue(IntArrays.equals(array1, 0, array4, 0, 100));
        assertEquals(100, array4.length);

        // new copy preserving all available
        var array5 = ensureCapacity(array1, 120);
        assertTrue(IntArrays.equals(array1, 0, array5, 0, 100));
        assertEquals(120, array5.length);

        // new copy preserving 10
        var array6 = ensureCapacity(array1, 120, 10);
        assertTrue(IntArrays.equals(array1, 0, array6, 0, 10));
        assertTrue(IntArrays.equals(newFill(90, 0), 0, array6, 10, 90));

        // leave untouched
        var array7 = grow(array1, 10);
        assertTrue(IntArrays.equals(array1, 0, array7, 0, 100));
        assertEquals(100, array7.length);

        // new copy preserving all
        var array8 = grow(array1, 120);
        assertTrue(IntArrays.equals(array1, 0, array8, 0, 100));
        assertEquals(150, array8.length);

        // new copy preserving 10
        var array9 = grow(array1, 200, 10);
        assertTrue(IntArrays.equals(array1, 0, array9, 0, 10));
        assertTrue(IntArrays.equals(newFill(190, 0), 0, array9, 10, 190));
        assertEquals(200, array9.length);

        // trim array to 10
        var array10 = trim(array1, 10);
        assertEquals(10, array10.length);
        assertTrue(IntArrays.equals(array1, 0, array10, 0, 10));
    }

    @Test
    void testSorting() {

        int len = 100_000;
        var a = IntArrays.newSeq(0, len);
        var b = IntArrays.newSeq(0, len);

        assertAsc(a, IntArrays::quickSort);
        assertAsc(a, IntArrays::parallelQuickSort);

        assertDesc(a, IntArrays::quickSort);
        assertDesc(a, IntArrays::parallelQuickSort);

        assertAscIndirect(a, IntArrays::quickSortIndirect);
        assertAscIndirect(a, IntArrays::parallelQuickSortIndirect);

        assertAsc(a, IntArrays::mergeSort);
        assertDesc(a, IntArrays::mergeSort);

        assertAsc(a, IntArrays::parallelQuickSort);
        assertAsc2(a, b, IntArrays::quickSort);
        assertAsc2(a, b, IntArrays::parallelQuickSort);
    }

    private void assertAsc(int[] src, Consumer<int[]> fun) {
        var s = IntArrays.copy(src, 0, src.length);
        fun.accept(s);
        for (int i = 1; i < s.length; i++) {
            assertTrue(s[i - 1] <= s[i]);
        }
    }

    private void assertDesc(int[] src, BiConsumer<int[], IntComparator> fun) {
        var s = IntArrays.copy(src, 0, src.length);
        fun.accept(s, IntComparators.OPPOSITE_COMPARATOR);
        for (int i = 1; i < s.length; i++) {
            assertTrue(s[i - 1] >= s[i]);
        }
    }

    private void assertAsc2(int[] a, int[] b, BiConsumer<int[], int[]> alg) {
        var sa = IntArrays.copy(a, 0, a.length);
        var sb = IntArrays.copy(b, 0, b.length);
        alg.accept(sa, sb);
        for (int i = 1; i < sa.length; i++) {
            if (sa[i - 1] == sa[i]) {
                assertTrue(sb[i - 1] <= sb[i]);
            } else {
                assertTrue(sa[i - 1] < sa[i]);
            }
        }
    }

    private void assertAscIndirect(int[] array, BiConsumer<int[], int[]> alg) {
        int[] perm = IntArrays.newSeq(0, array.length);
        alg.accept(perm, array);
        for (int i = 1; i < array.length; i++) {
            assertTrue(array[perm[i - 1]] <= array[perm[i]]);
        }
    }

}
