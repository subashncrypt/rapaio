/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2021 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.math.linear;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static rapaio.sys.With.copy;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import rapaio.math.MathTools;
import rapaio.math.linear.base.DVectorBase;
import rapaio.math.linear.dense.DMatrixDenseC;
import rapaio.math.linear.dense.DMatrixDenseR;
import rapaio.math.linear.dense.DVectorDense;
import rapaio.math.linear.dense.DVectorStride;
import rapaio.sys.With;
import rapaio.util.collection.DoubleArrays;
import rapaio.util.collection.IntArrays;

public class DenseAlgebraTest {

    private static final MatrixFactory[] mTypes = new MatrixFactory[] {
            (rows, cols) -> {
                DMatrix m = new DMatrixDenseR(rows, cols);
                setValues(m, rows, cols);
                return m;
            },
            (rows, cols) -> {
                DMatrix m = new DMatrixDenseC(rows, cols);
                setValues(m, rows, cols);
                return m;
            },
            (rows, cols) -> {
                DMatrix m = new DMatrixDenseR(rows, cols);
                setValues(m, rows, cols);
                return m
                        .mapRows(IntArrays.newSeq(rows))
                        .mapCols(IntArrays.newSeq(cols))
                        .mapRows(IntArrays.newSeq(rows));
            }
    };
    private static final VectorFactory[] vectorFactories = new VectorFactory[] {
            // base vector
            n -> new DVectorBase(DoubleArrays.newSeq(1, n + 1)),
            // dense vector
            n -> {
                double[] values = new double[10 + n];
                for (int i = 0; i < n; i++) {
                    values[10 + i] = i + 1;
                }
                return new DVectorDense(10, n, values);
            },
            // stride vector
            n -> {
                double[] values = new double[10 + 2 * n];
                for (int i = 0; i < n; i++) {
                    values[10 + i * 2] = i + 1;
                }
                return new DVectorStride(10, 2, n, values);
            },
            // map vector
            n -> {
                double[] values = new double[10 + 2 * n];
                int[] indexes = new int[n];
                for (int i = 0; i < n; i++) {
                    values[10 + i * 2] = i + 1;
                    indexes[i] = 10 + i * 2;
                }
                return new DVectorDense(0, 10 + 2 * n, values).map(indexes);
            }
    };

    interface VectorFactory {
        default DVector newInstance() {
            return newInstance(10);
        }

        DVector newInstance(int size);
    }

    interface MatrixFactory {
        default DMatrix newMatrix() {
            return newMatrix(10, 10);
        }

        DMatrix newMatrix(int rows, int cols);
    }

    public static void setValues(DMatrix matrix, int m, int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                matrix.set(i, j, i * m + j + 1);
            }
        }
    }

    private interface F1m {
        void apply(DMatrix m);
    }

    private void t1m(MatrixFactory mType, F1m f) {
        f.apply(mType.newMatrix());
    }

    @Test
    void testOneMatrix() {
        for (MatrixFactory mType : mTypes) {
            String message = "matrix type: %s".formatted(mType.newMatrix().getClass().getName());
            t1m(mType, m -> assertTrue(m.add(10, copy()).deepEquals(m.add(10))));
            t1m(mType, m -> assertTrue(m.sub(10, copy()).deepEquals(m.sub(10))));
            t1m(mType, m -> assertTrue(m.mul(10, copy()).deepEquals(m.mul(10))));
            t1m(mType, m -> assertTrue(m.div(10, copy()).deepEquals(m.div(10))));

            t1m(mType, m -> assertEquals(IntStream.range(0, 10).mapToObj(i -> i * 11 + 1).mapToInt(i -> i).sum(), m.trace()));

            t1m(mType, m -> assertTrue(
                    DVector.wrap(IntStream.range(0, 10).mapToDouble(i -> i * 11 + 1).toArray()).deepEquals(m.diag())));

            t1m(mType, m -> assertTrue(m.scatter().deepEquals(m.copy().scatter())));

            t1m(mType, m -> assertEquals(5050, m.sum()));
            t1m(mType, m -> assertEquals(m.sum(0).sum(), m.sum(1).sum()));
            t1m(mType, m -> assertEquals(841.666666666666668, m.variance()));
            t1m(mType, m -> assertTrue(DVectorDense.fill(10, 916.66666666666668).deepEquals(m.variance(0)), message));
            t1m(mType, m -> assertTrue(DVectorDense.fill(10, 9.166666666666668).deepEquals(m.variance(1))));

            t1m(mType, m -> assertTrue(m.mapValues(m.argmax(0), 0).deepEquals(m.max(0)), message));
            t1m(mType, m -> assertTrue(m.mapValues(m.argmax(1), 1).deepEquals(m.max(1))));
            t1m(mType, m -> assertTrue(m.mapValues(m.argmin(0), 0).deepEquals(m.min(0))));
            t1m(mType, m -> assertTrue(m.mapValues(m.argmin(1), 1).deepEquals(m.min(1)), message));

            t1m(mType, m -> assertTrue(m.apply(MathTools::sqrt, With.copy()).deepEquals(m.apply(MathTools::sqrt))));
            t1m(mType, m -> assertTrue(m.t(copy()).t(copy()).deepEquals(m.t().t())));
            t1m(mType, m -> assertEquals(m.sum(), m.valueStream().sum()));

            t1m(mType, m -> assertTrue(m.deepEquals(m.resizeCopy(10, 10, 100))));
            t1m(mType, m -> assertEquals(m.sum(), m.resizeCopy(19, 19, 0).sum()));
            t1m(mType, m -> assertEquals(m.rangeCols(0, 5).rangeRows(0, 5).sum(), m.resizeCopy(5, 5, 0).sum()));

            t1m(mType, m -> assertEquals(m.getClass().getSimpleName() + """
                    {rowCount:10, colCount:10, values:
                    [
                     [  1  2  3  4  5  6  7  8  9  10 ],\s
                     [ 11 12 13 14 15 16 17 18 19  20 ],\s
                     [ 21 22 23 24 25 26 27 28 29  30 ],\s
                     [ 31 32 33 34 35 36 37 38 39  40 ],\s
                     [ 41 42 43 44 45 46 47 48 49  50 ],\s
                     [ 51 52 53 54 55 56 57 58 59  60 ],\s
                     [ 61 62 63 64 65 66 67 68 69  70 ],\s
                     [ 71 72 73 74 75 76 77 78 79  80 ],\s
                     [ 81 82 83 84 85 86 87 88 89  90 ],\s
                     [ 91 92 93 94 95 96 97 98 99 100 ],\s
                    ]}""", m.toString()));

            t1m(mType, m -> assertEquals("""
                        [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]\s
                    [0]  1   2   3   4   5   6   7   8   9   10 [4] 41  42  43  44  45  46  47  48  49   50 [8] 81  82  83  84  85  86  87  88  89   90\s
                    [1] 11  12  13  14  15  16  17  18  19   20 [5] 51  52  53  54  55  56  57  58  59   60 [9] 91  92  93  94  95  96  97  98  99  100\s
                    [2] 21  22  23  24  25  26  27  28  29   30 [6] 61  62  63  64  65  66  67  68  69   70\s
                    [3] 31  32  33  34  35  36  37  38  39   40 [7] 71  72  73  74  75  76  77  78  79   80\s
                    """, m.toContent()));
            t1m(mType, m -> assertEquals("""
                        [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]\s
                    [0]  1   2   3   4   5   6   7   8   9   10 [4] 41  42  43  44  45  46  47  48  49   50 [8] 81  82  83  84  85  86  87  88  89   90\s
                    [1] 11  12  13  14  15  16  17  18  19   20 [5] 51  52  53  54  55  56  57  58  59   60 [9] 91  92  93  94  95  96  97  98  99  100\s
                    [2] 21  22  23  24  25  26  27  28  29   30 [6] 61  62  63  64  65  66  67  68  69   70\s
                    [3] 31  32  33  34  35  36  37  38  39   40 [7] 71  72  73  74  75  76  77  78  79   80\s
                    """, m.toFullContent()));

            t1m(mType, m -> assertEquals("""
                        [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]     [0] [1] [2] [3] [4] [5] [6] [7] [8] [9]\s
                    [0]  1   2   3   4   5   6   7   8   9   10 [4] 41  42  43  44  45  46  47  48  49   50 [8] 81  82  83  84  85  86  87  88  89   90\s
                    [1] 11  12  13  14  15  16  17  18  19   20 [5] 51  52  53  54  55  56  57  58  59   60 [9] 91  92  93  94  95  96  97  98  99  100\s
                    [2] 21  22  23  24  25  26  27  28  29   30 [6] 61  62  63  64  65  66  67  68  69   70\s
                    [3] 31  32  33  34  35  36  37  38  39   40 [7] 71  72  73  74  75  76  77  78  79   80\s
                    """, m.toSummary()));

            assertEquals("""
                           [0]   [1]   [2]   [3]   [4]   [5]   [6]   [7]   [8]   [9]  [10]  [11]  [12]  [13]  [14]  [15]  [16]  [17]  [18]  [19] ...  [98]   [99]\s
                     [0]     1     2     3     4     5     6     7     8     9    10    11    12    13    14    15    16    17    18    19    20 ...    99    100\s
                     [1]   101   102   103   104   105   106   107   108   109   110   111   112   113   114   115   116   117   118   119   120 ...   199    200\s
                     [2]   201   202   203   204   205   206   207   208   209   210   211   212   213   214   215   216   217   218   219   220 ...   299    300\s
                     [3]   301   302   303   304   305   306   307   308   309   310   311   312   313   314   315   316   317   318   319   320 ...   399    400\s
                     [4]   401   402   403   404   405   406   407   408   409   410   411   412   413   414   415   416   417   418   419   420 ...   499    500\s
                     [5]   501   502   503   504   505   506   507   508   509   510   511   512   513   514   515   516   517   518   519   520 ...   599    600\s
                     [6]   601   602   603   604   605   606   607   608   609   610   611   612   613   614   615   616   617   618   619   620 ...   699    700\s
                     [7]   701   702   703   704   705   706   707   708   709   710   711   712   713   714   715   716   717   718   719   720 ...   799    800\s
                     [8]   801   802   803   804   805   806   807   808   809   810   811   812   813   814   815   816   817   818   819   820 ...   899    900\s
                     [9]   901   902   903   904   905   906   907   908   909   910   911   912   913   914   915   916   917   918   919   920 ...   999  1,000\s
                    [10] 1,001 1,002 1,003 1,004 1,005 1,006 1,007 1,008 1,009 1,010 1,011 1,012 1,013 1,014 1,015 1,016 1,017 1,018 1,019 1,020 ... 1,099  1,100\s
                    [11] 1,101 1,102 1,103 1,104 1,105 1,106 1,107 1,108 1,109 1,110 1,111 1,112 1,113 1,114 1,115 1,116 1,117 1,118 1,119 1,120 ... 1,199  1,200\s
                    [12] 1,201 1,202 1,203 1,204 1,205 1,206 1,207 1,208 1,209 1,210 1,211 1,212 1,213 1,214 1,215 1,216 1,217 1,218 1,219 1,220 ... 1,299  1,300\s
                    [13] 1,301 1,302 1,303 1,304 1,305 1,306 1,307 1,308 1,309 1,310 1,311 1,312 1,313 1,314 1,315 1,316 1,317 1,318 1,319 1,320 ... 1,399  1,400\s
                    [14] 1,401 1,402 1,403 1,404 1,405 1,406 1,407 1,408 1,409 1,410 1,411 1,412 1,413 1,414 1,415 1,416 1,417 1,418 1,419 1,420 ... 1,499  1,500\s
                    [15] 1,501 1,502 1,503 1,504 1,505 1,506 1,507 1,508 1,509 1,510 1,511 1,512 1,513 1,514 1,515 1,516 1,517 1,518 1,519 1,520 ... 1,599  1,600\s
                    [16] 1,601 1,602 1,603 1,604 1,605 1,606 1,607 1,608 1,609 1,610 1,611 1,612 1,613 1,614 1,615 1,616 1,617 1,618 1,619 1,620 ... 1,699  1,700\s
                    [17] 1,701 1,702 1,703 1,704 1,705 1,706 1,707 1,708 1,709 1,710 1,711 1,712 1,713 1,714 1,715 1,716 1,717 1,718 1,719 1,720 ... 1,799  1,800\s
                    [18] 1,801 1,802 1,803 1,804 1,805 1,806 1,807 1,808 1,809 1,810 1,811 1,812 1,813 1,814 1,815 1,816 1,817 1,818 1,819 1,820 ... 1,899  1,900\s
                    [19] 1,901 1,902 1,903 1,904 1,905 1,906 1,907 1,908 1,909 1,910 1,911 1,912 1,913 1,914 1,915 1,916 1,917 1,918 1,919 1,920 ... 1,999  2,000\s
                    ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...   ...  ...  ...   ...  \s
                    [98] 9,801 9,802 9,803 9,804 9,805 9,806 9,807 9,808 9,809 9,810 9,811 9,812 9,813 9,814 9,815 9,816 9,817 9,818 9,819 9,820 ... 9,899  9,900\s
                    [99] 9,901 9,902 9,903 9,904 9,905 9,906 9,907 9,908 9,909 9,910 9,911 9,912 9,913 9,914 9,915 9,916 9,917 9,918 9,919 9,920 ... 9,999 10,000\s
                    """, mType.newMatrix(100, 100).toContent());
        }
    }

    private interface F1m1v {
        void apply(DMatrix m, DVector v);
    }

    void t1m1v(MatrixFactory mType, VectorFactory vf, F1m1v f) {
        DMatrix m = mType.newMatrix();
        DVector v = vf.newInstance();
        f.apply(m, v);
    }

    @Test
    void testOneMatrixOneVector() {
        for (MatrixFactory mType : mTypes) {
            for (var vf : vectorFactories) {
                t1m1v(mType, vf, (m, v) -> assertTrue(m.add(v, 0, copy()).deepEquals(m.add(v, 0))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.add(v, 1, copy()).deepEquals(m.add(v, 1))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.sub(v, 0, copy()).deepEquals(m.sub(v, 0))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.sub(v, 1, copy()).deepEquals(m.sub(v, 1))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.mul(v, 0, copy()).deepEquals(m.mul(v, 0))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.mul(v, 1, copy()).deepEquals(m.mul(v, 1))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.div(v, 0, copy()).deepEquals(m.div(v, 0))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.div(v, 1, copy()).deepEquals(m.div(v, 1))));
                t1m1v(mType, vf, (m, v) -> assertTrue(m.dot(v).deepEquals(m.copy().dot(v.copy()))));
            }
        }
    }

    private interface F2m {
        void apply(DMatrix m1, DMatrix m2);
    }

    private void t2m(MatrixFactory mType1, MatrixFactory mType2, F2m f) {
        DMatrix m1 = mType1.newMatrix();
        DMatrix m2 = mType2.newMatrix();
        f.apply(m1, m2);
    }

    @Test
    void testTwoMatrices() {

        for (MatrixFactory mType1 : mTypes) {
            for (MatrixFactory mType2 : mTypes) {
                String message = "m1 type: %s, m2 type: %s".formatted(
                        mType1.newMatrix().getClass().getName(),
                        mType2.newMatrix().getClass().getName());
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.add(m2, copy()).deepEquals(m1.add(m2))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.sub(m2, copy()).deepEquals(m1.sub(m2))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.mul(m2, copy()).deepEquals(m1.mul(m2))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.div(m2, copy()).deepEquals(m1.div(m2))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.dot(m2).deepEquals(m1.copy().dot(m2.copy())), message));
                int[] in = new int[] {0, 1, 2, 3, 4};
                int[] out = new int[] {5, 6, 7, 8, 9};
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.mapRows(in).deepEquals(m2.removeRows(out))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.mapRowsNew(in).deepEquals(m2.removeRowsNew(out))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.mapCols(in).deepEquals(m2.removeCols(out))));
                t2m(mType1, mType2, (m1, m2) -> assertTrue(m1.mapColsNew(in).deepEquals(m2.removeColsNew(out))));
            }
        }
    }

    private interface F1v {
        void apply(DVector v);
    }

    private void t1v(VectorFactory vf, F1v f) {
        DVector v = vf.newInstance();
        f.apply(v);
    }

    @Test
    void testOneVector() {

        for (var vf : vectorFactories) {
            String message = "vector type: %s".formatted(vf.newInstance().getClass().getName());
            t1v(vf, v -> assertTrue(v.addNew(10).deepEquals(v.add(10))));
            t1v(vf, v -> assertTrue(v.subNew(10).deepEquals(v.sub(10))));
            t1v(vf, v -> assertTrue(v.mulNew(10).deepEquals(v.mul(10))));
            t1v(vf, v -> assertTrue(v.divNew(10).deepEquals(v.div(10))));

            t1v(vf, v -> assertTrue(DVectorDense.fill(10, 7).deepEquals(v.copy().fill(7))));

            t1v(vf, v -> assertEquals(v.pnorm(1), v.sum()));
            t1v(vf, v -> assertEquals(10, v.pnorm(Double.POSITIVE_INFINITY)));
            t1v(vf, v -> assertEquals(55, v.sum()));
            t1v(vf, v -> assertEquals(55, v.nansum()));
            t1v(vf, v -> assertEquals(3628800, v.prod()));
            t1v(vf, v -> assertEquals(3628800, v.nanprod()));
            t1v(vf, v -> assertEquals(10, v.nancount()));
            t1v(vf, v -> assertEquals(5.5, v.mean()));
            t1v(vf, v -> assertEquals(5.5, v.nanmean()));
            t1v(vf, v -> assertEquals(9.166666666666666, v.variance()));
            t1v(vf, v -> assertEquals(9.166666666666666, v.nanvariance()));

            t1v(vf, v -> assertEquals(0, v.argmin()));
            t1v(vf, v -> assertEquals(1, v.min()));
            t1v(vf, v -> assertEquals(9, v.argmax()));
            t1v(vf, v -> assertEquals(10, v.max()));

            t1v(vf, v -> assertTrue(v.applyNew((row, x) -> row * x).deepEquals(v.apply((row, x) -> row * x))));

            int[] indexes = new int[] {2, 7, 3, 2};
            t1v(vf, v -> assertArrayEquals(indexes, v.map(indexes).valueStream().mapToInt(x -> (int) x - 1).toArray(), message));
            t1v(vf, v -> assertArrayEquals(indexes, v.mapNew(indexes).valueStream().mapToInt(x -> (int) x - 1).toArray(), message));

            t1v(vf, v -> assertTrue(v.applyNew(x -> x + 1).deepEquals(v.apply(x -> x + 1))));

            t1v(vf, v -> {
                DVector cumsum = v.copy();
                for (int i = 1; i < cumsum.size(); i++) {
                    cumsum.inc(i, cumsum.get(i - 1));
                }
                assertTrue(cumsum.deepEquals(v.cumsum()));
            });

            t1v(vf, v -> {
                var cumprod = v.copy();
                for (int i = 1; i < cumprod.size(); i++) {
                    cumprod.set(i, cumprod.get(i) * cumprod.get(i - 1));
                }
                assertTrue(cumprod.deepEquals(v.cumprod()));
            });

            t1v(vf, v -> assertTrue(v.asMatrix().mapCol(0).deepEquals(v),
                    "vtype: %s".formatted(vf.newInstance().getClass().getName())));

            t1v(vf, v -> assertEquals(v.getClass().getSimpleName() + "{size:10, values:[1,2,3,4,5,6,7,8,9,10]}", v.toString()));
            t1v(vf, v -> assertEquals("""
                    [0]  1 [4]  5 [8]  9\s
                    [1]  2 [5]  6 [9] 10\s
                    [2]  3 [6]  7\s
                    [3]  4 [7]  8\s
                    """, v.toContent()));
            assertEquals("""
                     [0]   1  [6]   7 [12]  13 [18]  19\s
                     [1]   2  [7]   8 [13]  14 [19]  20\s
                     [2]   3  [8]   9 [14]  15 ...  ...\s
                     [3]   4  [9]  10 [15]  16 [98]  99\s
                     [4]   5 [10]  11 [16]  17 [99] 100\s
                     [5]   6 [11]  12 [17]  18\s
                    """, vf.newInstance(100).toContent());
            t1v(vf, v -> assertEquals("""
                    [0]  1 [4]  5 [8]  9\s
                    [1]  2 [5]  6 [9] 10\s
                    [2]  3 [6]  7\s
                    [3]  4 [7]  8\s
                    """, v.toFullContent()));
            t1v(vf, v -> assertEquals("""
                    [0]  1 [4]  5 [8]  9\s
                    [1]  2 [5]  6 [9] 10\s
                    [2]  3 [6]  7\s
                    [3]  4 [7]  8\s
                    """, v.toSummary()));

            t1v(vf, v -> {
                v = v.copy().sortValues();
                for (int i = 1; i < v.size(); i++) {
                    assertTrue(v.get(i - 1) <= v.get(i));
                }
            });
        }
    }

    @Test
    void testTwoVectors() {
        for (VectorFactory vf1 : vectorFactories) {
            for (VectorFactory vf2 : vectorFactories) {
                DVector v1 = vf1.newInstance();
                DVector v2 = vf2.newInstance();

                String msg = String.format("type1: %s, type2: %s", v1.getClass().getName(), v2.getClass().getName());
                assertTrue(v1.addNew(v2).deepEquals(v1.add(v2)), msg);
                assertTrue(v1.subNew(v2).deepEquals(v1.sub(v2)), msg);
                assertTrue(v1.mulNew(v2).deepEquals(v1.mul(v2)), msg);
                assertTrue(v1.divNew(v2).deepEquals(v1.div(v2)), msg);

                v1 = vf1.newInstance();
                v2 = vf2.newInstance();
                assertTrue(v1.addMulNew(10, v2).deepEquals(v1.addMul(10, v2)), msg);

                v1 = vf1.newInstance();
                v2 = vf2.newInstance();

                assertEquals(385.0, v1.dot(v2));
                assertEquals(385.0, v1.dotBilinear(DMatrix.identity(10), v2));
                assertEquals(385.0, v1.dotBilinear(DMatrix.identity(10)));
                assertEquals(385.0, v1.dotBilinearDiag(DMatrix.identity(10), v2));
                assertEquals(385.0, v1.dotBilinearDiag(DVector.ones(10)));

                DMatrix m = v1.outer(v2);
                assertEquals(v1.size(), m.rowCount());
                assertEquals(v2.size(), m.colCount());
                for (int i = 0; i < v1.size(); i++) {
                    for (int j = 0; j < v2.size(); j++) {
                        assertEquals(v1.get(i) * v2.get(j), m.get(i, j));
                    }
                }
            }
        }
    }

    private interface F1m2v {
        void apply(DVector v1, DVector v2, DMatrix m);
    }

    private void t1m2v(VectorFactory vf1, VectorFactory vf2, MatrixFactory mType, F1m2v f) {
        DVector v1 = vf1.newInstance();
        DVector v2 = vf2.newInstance();
        DMatrix m = mType.newMatrix();
        f.apply(v1, v2, m);
    }

    @Test
    void testOneMatrixTwoVectors() {
        for (var vf1 : vectorFactories) {
            for (var vf2 : vectorFactories) {
                for (MatrixFactory mType1 : mTypes) {
                    t1m2v(vf1, vf2, mType1, (v1, v2, m)
                            -> assertEquals(v1.copy().mul(m.diag()).mul(v2).sum(), v1.dotBilinearDiag(m, v2)));
                    t1m2v(vf1, vf2, mType1, (v1, v2, m)
                            -> assertEquals(v1.copy().mul(m.diag()).mul(v1).sum(), v1.dotBilinearDiag(m)));
                    t1m2v(vf1, vf2, mType1, (v1, v2, m)
                            -> assertEquals(v1.copy().mul(v1).mul(v2).sum(), v1.dotBilinearDiag(v2, v1)));
                    t1m2v(vf1, vf2, mType1, (v1, v2, m)
                            -> assertEquals(v1.copy().mul(v1).mul(v2).sum(), v1.dotBilinearDiag(v2)));
                }
            }
        }
    }
}