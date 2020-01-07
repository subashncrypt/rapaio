package rapaio.util.collection;


import rapaio.util.function.DoubleDoubleFunction;
import rapaio.util.function.IntDoubleFunction;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 * Utility class to handle the manipulation of arrays of double 64 floating values.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/11/19.
 */
public final class DoubleArrays {

    private DoubleArrays() {
    }

    /**
     * Creates a double array filled with a given value
     *
     * @param size      size of the array
     * @param fillValue value to fill the array
     * @return new array instance
     */
    public static double[] newFill(int size, double fillValue) {
        double[] array = new double[size];
        if (fillValue != 0) {
            Arrays.fill(array, fillValue);
        }
        return array;
    }

    /**
     * Creates a new array filled with a sequence of values starting from
     * {@param start} (inclusive) and ending with {@param end} (exclusive)
     *
     * @param start sequence starting value (inclusive)
     * @param end   sequence ending value (exclusive)
     * @return array with sequence values
     */
    public static double[] newSeq(int start, int end) {
        double[] data = new double[end - start];
        for (int i = 0; i < end - start; i++) {
            data[i] = start + i;
        }
        return data;
    }

    /**
     * Builds a new double array with values from the given chunk transformed
     * with a function.
     *
     * @param source source array
     * @param start  starting position from source array (inclusive)
     * @param end    ending position from source array (exclusive)
     * @param fun    transforming function
     * @return transformed values array
     */
    public static double[] newFrom(double[] source, int start, int end, DoubleDoubleFunction fun) {
        double[] data = new double[end - start];
        for (int i = start; i < end; i++) {
            data[i - start] = fun.applyAsDouble(source[i]);
        }
        return data;
    }

    /**
     * Builds a new double array with values from the given chunk transformed
     * with a function.
     *
     * @param start starting position from source array (inclusive)
     * @param end   ending position from source array (exclusive)
     * @param fun   transforming function
     * @return transformed values array
     */
    public static double[] newFrom(int start, int end, IntDoubleFunction fun) {
        double[] data = new double[end - start];
        for (int i = start; i < end; i++) {
            data[i - start] = fun.applyAsDouble(i);
        }
        return data;
    }

    public static double[] newCopy(double[] array, int start, int end) {
        double[] data = new double[end - start];
        System.arraycopy(array, start, data, 0, end - start);
        return data;
    }

    /**
     * Verifies if the size of the array is at least as large as desired size.
     *
     * @param array input array
     * @param size  desired size
     * @return true if the length of the array is greater or equal than desired size
     */
    public static boolean checkCapacity(double[] array, int size) {
        return size <= array.length;
    }

    /**
     * Check if the array size is enough to store an element at given {@param pos}.
     * If it is enough capacity it returns the same array. If it is not enough,
     * a new array copy is created with an increasing factor of 1.5 of the
     * original size.
     *
     * @param array initial array
     * @param size  size of the array which must be ensured
     * @return adjusted capacity array if modified, old instance if not
     */
    public static double[] ensureCapacity(double[] array, int size) {
        if (size < array.length) {
            return array;
        }
        double[] data = new double[Math.max(size, array.length + (array.length >> 1))];
        System.arraycopy(array, 0, data, 0, array.length);
        return data;
    }

    /**
     * Delete element from given position by copying subsequent elements one position ahead.
     *
     * @param array source array of elements
     * @param size  the length of the array with known values
     * @param pos   position of the element to be removed
     * @return same int array
     */
    public static double[] delete(double[] array, int size, int pos) {
        if (size - pos > 0) {
            System.arraycopy(array, pos + 1, array, pos, size - pos - 1);
        }
        return array;
    }

    private static final int QUICKSORT_NO_REC = 16;
    private static final int QUICKSORT_MEDIAN_OF_9 = 128;

    /**
     * Swaps two elements of an anrray.
     *
     * @param x an array.
     * @param a a position in {@code x}.
     * @param b another position in {@code x}.
     */
    public static void swap(final double[] x, final int a, final int b) {
        final double t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    /**
     * Swaps two sequences of elements of an array.
     *
     * @param x an array.
     * @param a a position in {@code x}.
     * @param b another position in {@code x}.
     * @param n the number of elements to exchange starting at {@code a} and
     *          {@code b}.
     */
    public static void swap(final double[] x, int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++)
            swap(x, a, b);
    }

    private static int med3(final double[] x, final int a, final int b, final int c, DoubleComparator comp) {
        final int ab = comp.compare(x[a], x[b]);
        final int ac = comp.compare(x[a], x[c]);
        final int bc = comp.compare(x[b], x[c]);
        return (ab < 0 ? (bc < 0 ? b : ac < 0 ? c : a) : (bc > 0 ? b : ac > 0 ? c : a));
    }

    private static void selectionSort(final double[] a, final int from, final int to, final DoubleComparator comp) {
        for (int i = from; i < to - 1; i++) {
            int m = i;
            for (int j = i + 1; j < to; j++)
                if (comp.compare(a[j], a[m]) < 0)
                    m = j;
            if (m != i) {
                final double u = a[i];
                a[i] = a[m];
                a[m] = u;
            }
        }
    }

    /**
     * Sorts the specified range of elements according to the order induced by the
     * specified comparator using quicksort.
     *
     * <p>
     * The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M.
     * Douglas McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software:
     * Practice and Experience</i>, 23(11), pages 1249&minus;1265, 1993.
     *
     * <p>
     * Note that this implementation does not allocate any object, contrarily to the
     * implementation used to sort primitive types in {@link java.util.Arrays},
     * which switches to mergesort on large inputs.
     *
     * @param x    the array to be sorted.
     * @param from the index of the first element (inclusive) to be sorted.
     * @param to   the index of the last element (exclusive) to be sorted.
     * @param comp the comparator to determine the sorting order.
     */
    public static void quickSort(final double[] x, final int from, final int to, final DoubleComparator comp) {
        final int len = to - from;
        // Selection sort on smallest arrays
        if (len < QUICKSORT_NO_REC) {
            selectionSort(x, from, to, comp);
            return;
        }
        // Choose a partition element, v
        int m = from + len / 2;
        int l = from;
        int n = to - 1;
        if (len > QUICKSORT_MEDIAN_OF_9) { // Big arrays, pseudomedian of 9
            int s = len / 8;
            l = med3(x, l, l + s, l + 2 * s, comp);
            m = med3(x, m - s, m, m + s, comp);
            n = med3(x, n - 2 * s, n - s, n, comp);
        }
        m = med3(x, l, m, n, comp); // Mid-size, med of 3
        final double v = x[m];
        // Establish Invariant: v* (<v)* (>v)* v*
        int a = from, b = a, c = to - 1, d = c;
        while (true) {
            int comparison;
            while (b <= c && (comparison = comp.compare(x[b], v)) <= 0) {
                if (comparison == 0)
                    swap(x, a++, b);
                b++;
            }
            while (c >= b && (comparison = comp.compare(x[c], v)) >= 0) {
                if (comparison == 0)
                    swap(x, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, b++, c--);
        }
        // Swap partition elements back to middle
        int s;
        s = Math.min(a - from, b - a);
        swap(x, from, b - s, s);
        s = Math.min(d - c, to - d - 1);
        swap(x, b, to - s, s);
        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            quickSort(x, from, from + s, comp);
        if ((s = d - c) > 1)
            quickSort(x, to - s, to, comp);
    }

    /**
     * Sorts the specified range of elements according to the natural ascending
     * order using indirect quicksort.
     *
     * <p>
     * The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M.
     * Douglas McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software:
     * Practice and Experience</i>, 23(11), pages 1249&minus;1265, 1993.
     *
     * <p>
     * This method implement an <em>indirect</em> sort. The elements of {@code perm}
     * (which must be exactly the numbers in the interval {@code [0..perm.length)})
     * will be permuted so that {@code x[perm[i]] &le; x[perm[i + 1]]}.
     *
     * <p>
     * Note that this implementation does not allocate any object, contrarily to the
     * implementation used to sort primitive types in {@link java.util.Arrays},
     * which switches to mergesort on large inputs.
     *
     * @param perm a permutation array indexing {@code x}.
     * @param x    the array to be sorted.
     * @param from the index of the first element (inclusive) to be sorted.
     * @param to   the index of the last element (exclusive) to be sorted.
     */

    public static void quickSortIndirect(final int[] perm, final double[] x, final int from, final int to) {
        final int len = to - from;
        // Selection sort on smallest arrays
        if (len < QUICKSORT_NO_REC) {
            insertionSortIndirect(perm, x, from, to);
            return;
        }
        // Choose a partition element, v
        int m = from + len / 2;
        int l = from;
        int n = to - 1;
        if (len > QUICKSORT_MEDIAN_OF_9) { // Big arrays, pseudomedian of 9
            int s = len / 8;
            l = med3Indirect(perm, x, l, l + s, l + 2 * s);
            m = med3Indirect(perm, x, m - s, m, m + s);
            n = med3Indirect(perm, x, n - 2 * s, n - s, n);
        }
        m = med3Indirect(perm, x, l, m, n); // Mid-size, med of 3
        final double v = x[perm[m]];
        // Establish Invariant: v* (<v)* (>v)* v*
        int a = from, b = a, c = to - 1, d = c;
        while (true) {
            int comparison;
            while (b <= c && (comparison = (Double.compare((x[perm[b]]), (v)))) <= 0) {
                if (comparison == 0)
                    IntArrays.swap(perm, a++, b);
                b++;
            }
            while (c >= b && (comparison = (Double.compare((x[perm[c]]), (v)))) >= 0) {
                if (comparison == 0)
                    IntArrays.swap(perm, c, d--);
                c--;
            }
            if (b > c)
                break;
            IntArrays.swap(perm, b++, c--);
        }
        // Swap partition elements back to middle
        int s;
        s = Math.min(a - from, b - a);
        IntArrays.swapSeq(perm, from, b - s, s);
        s = Math.min(d - c, to - d - 1);
        IntArrays.swapSeq(perm, b, to - s, s);
        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            quickSortIndirect(perm, x, from, from + s);
        if ((s = d - c) > 1)
            quickSortIndirect(perm, x, to - s, to);
    }

    private static int med3Indirect(final int[] perm, final double[] x, final int a, final int b, final int c) {
        final double aa = x[perm[a]];
        final double bb = x[perm[b]];
        final double cc = x[perm[c]];
        final int ab = (Double.compare((aa), (bb)));
        final int ac = (Double.compare((aa), (cc)));
        final int bc = (Double.compare((bb), (cc)));
        return (ab < 0 ? (bc < 0 ? b : ac < 0 ? c : a) : (bc > 0 ? b : ac > 0 ? c : a));
    }

    private static void insertionSortIndirect(final int[] perm, final double[] a, final int from, final int to) {
        for (int i = from; ++i < to; ) {
            int t = perm[i];
            int j = i;
            for (int u = perm[j - 1]; (Double.compare((a[t]), (a[u])) < 0); u = perm[--j - 1]) {
                perm[j] = u;
                if (from == j - 1) {
                    --j;
                    break;
                }
            }
            perm[j] = t;
        }
    }

    /**
     * Shuffles the specified array fragment using the specified pseudorandom number generator.
     *
     * @param a      the array to be shuffled.
     * @param from   first element inclusive
     * @param to     last element exclusive
     * @param random a pseudorandom number generator.
     */
    public static void shuffle(final double[] a, final int from, final int to, final Random random) {
        for (int i = to - from; i-- != 0; ) {
            final int p = random.nextInt(i + 1);
            final double t = a[from + i];
            a[from + i] = a[from + p];
            a[from + p] = t;
        }
    }

    /**
     * Shuffles the specified array using the specified pseudorandom number generator.
     *
     * @param a      the array to be shuffled.
     * @param random a pseudorandom number generator.
     */
    public static void shuffle(final double[] a, final Random random) {
        for (int i = a.length; i-- != 0; ) {
            final int p = random.nextInt(i + 1);
            final double t = a[i];
            a[i] = a[p];
            a[p] = t;
        }
    }

    /**
     * Reverses the order of the elements in the specified array.
     *
     * @param a the array to be reversed.
     */
    public static void reverse(final double[] a) {
        final int length = a.length;
        for (int i = length / 2; i-- != 0; ) {
            final double t = a[length - i - 1];
            a[length - i - 1] = a[i];
            a[i] = t;
        }
    }

    /**
     * Reverses the order of the elements in the specified array fragment.
     *
     * @param a    the array to be reversed.
     * @param from the index of the first element (inclusive) to be reversed.
     * @param to   the index of the last element (exclusive) to be reversed.
     */
    public static void reverse(final double[] a, final int from, final int to) {
        final int length = to - from;
        for (int i = length / 2; i-- != 0; ) {
            final double t = a[from + length - i - 1];
            a[from + length - i - 1] = a[from + i];
            a[from + i] = t;
        }
    }

    public static DoubleStream stream(double[] array, int start, int end) {
        return Arrays.stream(array, start, end);
    }

    public static DoubleIterator iterator(double[] array, int start, int end) {
        return new DoubleIterator() {
            private int pos = start;

            @Override
            public boolean hasNext() {
                return pos < end;
            }

            @Override
            public double nextDouble() {
                if (pos >= end) {
                    throw new NoSuchElementException();
                }
                return array[pos++];
            }
        };
    }

    // plus

    public static double[] plus(double[] a, double s, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] += s;
        }
        return a;
    }

    public static double[] plusc(double[] a, double s, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] + s;
        }
        return array;
    }

    public static double[] plus(double[] a, double[] b, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] += b[i];
        }
        return a;
    }

    public static double[] plusc(double[] a, double[] b, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] + b[i];
        }
        return array;
    }

    // minus

    public static double[] minus(double[] a, double s, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] -= s;
        }
        return a;
    }

    public static double[] minusc(double[] a, double s, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] - s;
        }
        return array;
    }

    public static double[] minus(double[] a, double[] b, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] -= b[i];
        }
        return a;
    }

    public static double[] minusc(double[] a, double[] b, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] - b[i];
        }
        return array;
    }

    // dot

    public static double[] times(double[] a, double s, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] *= s;
        }
        return a;
    }

    public static double[] timesc(double[] a, double s, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] * s;
        }
        return array;
    }

    public static double[] times(double[] a, double[] b, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] *= b[i];
        }
        return a;
    }

    public static double[] timesc(double[] a, double[] b, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] * b[i];
        }
        return array;
    }

    // div

    public static double[] div(double[] a, double s, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] /= s;
        }
        return a;
    }

    public static double[] divc(double[] a, double s, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] / s;
        }
        return array;
    }

    public static double[] div(double[] a, double[] b, int start, int end) {
        for (int i = start; i < end; i++) {
            a[i] /= b[i];
        }
        return a;
    }

    public static double[] divc(double[] a, double[] b, int start, int end) {
        double[] array = new double[end - start];
        for (int i = start; i < end; i++) {
            array[i - start] = a[i] / b[i];
        }
        return array;
    }

    public static double sum(double[] array, int start, int end) {
        double sum = 0;
        for (int i = start; i < end; i++) {
            sum += array[i];
        }
        return sum;
    }

    public static double nansum(double[] array, int start, int end) {
        double sum = 0;
        for (int i = start; i < end; i++) {
            if (Double.isNaN(array[i])) {
                continue;
            }
            sum += array[i];
        }
        return sum;
    }

    public static int nancount(double[] array, int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (Double.isNaN(array[i])) {
                continue;
            }
            count++;
        }
        return count;
    }

    public static double mean(double[] array, int start, int end) {
        return sum(array, start, end) / (end - start);
    }

    public static double nanmean(double[] array, int start, int end) {
        double sum = 0;
        int count = 0;
        for (int i = start; i < end; i++) {
            if (Double.isNaN(array[i])) {
                continue;
            }
            sum += array[i];
            count++;
        }
        if (count == 0) {
            return Double.NaN;
        }
        return sum / count;
    }

    public static double variance(double[] array, int start, int end) {
        double mean = mean(array, start, end);
        int count = end - start;
        if (count == 0) {
            return Double.NaN;
        }
        double sum2 = 0;
        double sum3 = 0;
        for (int i = start; i < end; i++) {
            sum2 += Math.pow(array[i] - mean, 2);
            sum3 += array[i] - mean;
        }
        return (sum2 - Math.pow(sum3, 2) / count) / (count - 1.0);
    }

    public static double nanvariance(double[] array, int start, int end) {
        double mean = nanmean(array, start, end);
        int missingCount = 0;
        int completeCount = 0;
        for (int i = start; i < end; i++) {
            if (Double.isNaN(array[i])) {
                missingCount++;
            } else {
                completeCount++;
            }
        }
        if (completeCount == 0) {
            return Double.NaN;
        }
        double sum2 = 0;
        double sum3 = 0;
        for (int i = start; i < end; i++) {
            if (Double.isNaN(array[i])) {
                continue;
            }
            sum2 += Math.pow(array[i] - mean, 2);
            sum3 += array[i] - mean;
        }
        return (sum2 - Math.pow(sum3, 2) / completeCount) / (completeCount - 1.0);
    }
}
