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

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.DoubleStream;

import rapaio.core.distributions.Distribution;
import rapaio.core.distributions.Normal;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.math.linear.dense.DMatrixDense;
import rapaio.math.linear.dense.DMatrixDenseC;
import rapaio.math.linear.dense.DMatrixDenseR;
import rapaio.math.linear.option.AlgebraOption;
import rapaio.printer.Printable;
import rapaio.util.NotImplementedException;
import rapaio.util.function.Double2DoubleFunction;
import rapaio.util.function.IntInt2DoubleBiFunction;

/**
 * Dense matrix with double precision floating point values
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/3/16.
 */
public interface DMatrix extends Serializable, Printable {

    /**
     * Default storage type for a matrix. This storage type is used when no matrix storage type
     * is specified as parameter and there are multiple choices available for a given
     * operation.
     *
     * @return default storage type for a matrix
     */
    static MType defaultMType() {
        return MType.RDENSE;
    }

    /**
     * Creates a matrix with given {@code rows} and {@code cols} filled with values {@code 0}.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return new empty matrix with default storage type
     */
    static DMatrix empty(int rows, int cols) {
        return empty(defaultMType(), rows, cols);
    }

    /**
     * Creates a matrix with given {@code rows} and {@code cols} filled with values {@code 0}
     * with a storage type specified as a parameter.
     *
     * @param type matrix storage type
     * @param rows number of rows
     * @param cols number of columns
     * @return new empty matrix with given storage type
     */
    static DMatrix empty(MType type, int rows, int cols) {
        return switch (type) {
            case RDENSE -> new DMatrixDenseR(rows, cols);
            case CDENSE -> new DMatrixDenseC(rows, cols);
            default -> throw new NotImplementedException();
        };
    }

    /**
     * Creates an identity matrix with default storage type.
     * An identity matrix is a square matrix with {@code 0} values everywhere
     * except on the main diagonal where it is filled with {@code 1}
     *
     * @param n number of rows and columns of the square matrix
     * @return identity matrix of order n
     */
    static DMatrix identity(int n) {
        return identity(defaultMType(), n);
    }

    /**
     * Builds an identity matrix with n rows and n columns.
     * An identity matrix is a matrix with 1 on the main diagonal
     * and 0 otherwise.
     *
     * @param type matrix implementation storage type
     * @param n    number of rows and also number of columns
     * @return a new instance of identity matrix of order n
     */
    static DMatrix identity(MType type, int n) {
        DMatrix m = empty(type, n, n);
        for (int i = 0; i < n; i++) {
            m.set(i, i, 1.0);
        }
        return m;
    }

    static DMatrix diagonal(DVector v) {
        return diagonal(defaultMType(), v);
    }

    static DMatrix diagonal(MType type, DVector v) {
        DMatrix m = DMatrix.identity(type, v.size());
        for (int i = 0; i < v.size(); i++) {
            m.set(i, i, v.get(i));
        }
        return m;
    }

    /**
     * Builds a new matrix filled with a given value and with default storage type.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param fill fill value for all matrix cells
     * @return new matrix filled with value
     */
    static DMatrix fill(int rows, int cols, double fill) {
        return fill(defaultMType(), rows, cols, fill);
    }

    /**
     * Builds a new matrix filled with a given value and with specified storage type.
     *
     * @param type matrix implementation storage type
     * @param rows number of rows
     * @param cols number of columns
     * @param fill fill value for all matrix cells
     * @return new matrix filled with value
     */
    static DMatrix fill(MType type, int rows, int cols, double fill) {
        DMatrix m = empty(type, rows, cols);
        switch (type) {
            case RDENSE, CDENSE -> Arrays.fill(((DMatrixDense) m).getElements(), fill);
            default -> throw new NotImplementedException();
        }
        return m;
    }

    /**
     * Builds a new matrix filled with values computed by a given function
     * which receives as parameter the row and column of each element and
     * is stored in default storage format.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param fun  lambda function which computes a value given row and column positions
     * @return new matrix filled with value
     */
    static DMatrix fill(int rows, int cols, IntInt2DoubleBiFunction fun) {
        return fill(defaultMType(), rows, cols, fun);
    }

    /**
     * Builds a new matrix filled with values computed by a given function
     * which receives as parameter the row and column of each element and
     * is stored in specified storage format.
     *
     * @param type matrix implementation storage type
     * @param rows number of rows
     * @param cols number of columns
     * @param fun  lambda function which computes a value given row and column positions
     * @return new matrix filled with value
     */
    static DMatrix fill(MType type, int rows, int cols, IntInt2DoubleBiFunction fun) {
        DMatrix m = empty(type, rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, fun.applyIntIntAsDouble(i, j));
            }
        }
        return m;
    }

    /**
     * Build a matrix with values generated by a standard normal distribution and
     * is stored in the default storage format.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return matrix filled with random values
     */
    static DMatrix random(int rows, int cols) {
        return random(defaultMType(), rows, cols, Normal.std());
    }

    /**
     * Build a matrix with values generated by a standard normal distribution and
     * is stored in the specified storage format.
     *
     * @param type matrix storage type
     * @param rows number of rows
     * @param cols number of columns
     * @return matrix filled with random values
     */
    static DMatrix random(MType type, int rows, int cols) {
        return random(type, rows, cols, Normal.std());
    }

    /**
     * Build a matrix with values generated by a distribution given as parameter and
     * is stored in the specified storage format.
     *
     * @param type matrix storage type
     * @param rows number of rows
     * @param cols number of columns
     * @return matrix filled with random values
     */
    static DMatrix random(MType type, int rows, int cols, Distribution distribution) {
        return fill(type, rows, cols, (r, c) -> distribution.sampleNext());
    }

    /**
     * Builds a matrix which wraps an array of values. Because we have an array of values, the only storage types allowed
     * are {@link MType#CDENSE} and {@link MType#RDENSE}. The matrix storage type is chosen by {@code byRows} parameter value.
     * If {@code byRows} is {@code true} then the array is interpreted as rows values first and
     * {@link MType#RDENSE} storage matrix is chosen. If the value is {@code false} then the array
     * is interpreted as column values first and {@link MType#CDENSE} storage type is chosen.
     *
     * @param byRows true if we have an array of rows, false if we have an array of columns
     * @param values array of arrays of values
     * @return matrix which wrap the values
     */
    static DMatrix wrap(int rows, int cols, boolean byRows, double... values) {
        if (byRows) {
            return new DMatrixDenseR(rows, cols, values);
        } else {
            return new DMatrixDenseC(rows, cols, values);
        }
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type is the default type and values
     * are row oriented.
     *
     * @param values array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrix copy(double[][] values) {
        return copy(defaultMType(), 0, values.length, 0, values[0].length, true, values);
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type is the default type and values
     * are row or column oriented depending on the value of {@code byRows}.
     *
     * @param byRows true means row first orientation, otherwise column first orientation
     * @param values array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrix copy(boolean byRows, double[][] values) {
        return copy(defaultMType(), 0, values.length, 0, values[0].length, byRows, values);
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type and row/column
     * orientation are given as parameter.
     * <p>
     * The only storage matrix not allowed is {@link MType#MAP} since this matrix storage is not direct.
     *
     * @param type   matrix storage type
     * @param byRows if true values are row oriented, if false values are column oriented
     * @param values array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrix copy(MType type, boolean byRows, double[][] values) {
        return copy(type, 0, byRows ? values.length : values[0].length, 0, byRows ? values[0].length : values.length, byRows, values);
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type and row/column
     * orientation are given as parameter.
     * <p>
     * This is the most customizable way to transfer values from an array of arrays into a matrix.
     * The only storage matrix not allowed is {@link MType#MAP} since this matrix storage is not direct.
     * It allows creating of a matrix from a rectangular range of values.
     *
     * @param type     matrix storage type
     * @param byRows   if true values are row oriented, if false values are column oriented
     * @param rowStart starting row inclusive
     * @param rowEnd   end row exclusive
     * @param colStart column row inclusive
     * @param colEnd   column end exclusive
     * @param values   array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrix copy(MType type, int rowStart, int rowEnd, int colStart, int colEnd, boolean byRows, double[][] values) {
        if (type == MType.MAP) {
            throw new IllegalArgumentException("Matrix type not allowed.");
        }
        int rows = rowEnd - rowStart;
        int cols = colEnd - colStart;
        DMatrix m = empty(type, rows, cols);
        if (byRows) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    m.set(i, j, values[i + rowStart][j + colStart]);
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    m.set(i, j, values[j + colStart][i + rowStart]);
                }
            }
        }
        return m;
    }

    /**
     * Copies values from an array into a matrix with default row orientation and default storage type {@link MType#RDENSE}.
     * <p>
     * The layout of data is described by {@code inputRows} and {@code columnRows} and this is the same size
     * for the resulted matrix.
     *
     * @param inputRows number of rows for data layout
     * @param inputCols number of columns for data layout
     * @param values    array of values
     * @return matrix with a range of values copied from original array
     */
    static DMatrix copy(int inputRows, int inputCols, double... values) {
        return copy(defaultMType(), inputRows, inputCols, 0, inputRows, 0, inputCols, true, values);
    }

    static DMatrix copy(int inputRows, int inputCols, boolean byRows, double... values) {
        return copy(defaultMType(), inputRows, inputCols, 0, inputRows, 0, inputCols, byRows, values);
    }

    /**
     * Copies values from an array into a matrix.
     * <p>
     * All matrix storage types are allowed with the exeption of {@link MType#MAP} since that is an indirect
     * storage type.
     * <p>
     * The layout of data is described by {@code inputRows} and {@code columnRows}.
     * The row or column orientation is determined by {@code byRows} parameter. If {@code byRows} is true,
     * the values from the array are interpreted as containing rows one after another. If {@code byRows} is
     * false then the interpretation is that the array contains columns one after another.
     * <p>
     * The method creates an array of values of the same size as input data layout.
     *
     * @param type      matrix storage type
     * @param byRows    value orientation: true if row oriented, false if column oriented
     * @param inputRows number of rows for data layout
     * @param inputCols number of columns for data layout
     * @param values    array of values
     * @return matrix with a range of values copied from original array
     */
    static DMatrix copy(MType type, int inputRows, int inputCols, boolean byRows, double... values) {
        return copy(type, inputRows, inputCols, 0, inputRows, 0, inputCols, byRows, values);
    }

    /**
     * Copies values from an array into a matrix.
     * <p>
     * This is the most customizable way to copy values from a contiguous arrays into a matrix.
     * <p>
     * All matrix storage types are allowed with the exeption of {@link MType#MAP} since that is an indirect
     * storage type.
     * <p>
     * The layout of data is described by {@code inputRows} and {@code columnRows}.
     * The row or column orientation is determined by {@code byRows} parameter. If {@code byRows} is true,
     * the values from the array are interpreted as containing rows one after another. If {@code byRows} is
     * false then the interpretation is that the array contains columns one after another.
     * <p>
     * The method allows creation of an array using a contiguous range of rows and columns described by
     * parameters.
     *
     * @param type      matrix storage type
     * @param byRows    value orientation: true if row oriented, false if column oriented
     * @param inputRows number of rows for data layout
     * @param inputCols number of columns for data layout
     * @param rowStart  row start inclusive
     * @param rowEnd    row end exclusive
     * @param colStart  column start inclusive
     * @param colEnd    column end exclusive
     * @param values    array of values
     * @return matrix with a range of values copied from original array
     */
    static DMatrix copy(MType type, int inputRows, int inputCols,
            int rowStart, int rowEnd, int colStart, int colEnd, boolean byRows,
            double... values) {
        int rows = rowEnd - rowStart;
        int cols = colEnd - colStart;
        DMatrix m = empty(type, rows, cols);

        if (byRows) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    m.set(i, j, values[inputCols * (Math.max(0, rowStart - 1) + i) + colStart + j]);
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    m.set(i, j, values[inputRows * (Math.max(0, colStart - 1) + j) + rowStart + i]);
                }
            }
        }
        return m;
    }

    /**
     * Copies data from a data frame using the default data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param df data frame
     * @return matrix with collected values
     */
    static DMatrix copy(Frame df) {
        return copy(defaultMType(), df);
    }

    /**
     * Copies data from a data frame using the specified data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param type matrix storage type
     * @param df   data frame
     * @return matrix with collected values
     */
    static DMatrix copy(MType type, Frame df) {
        int rows = df.rowCount();
        int cols = df.varCount();
        DMatrix m = empty(type, rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, df.getDouble(i, j));
            }
        }
        return m;
    }

    /**
     * Copies data from a list of variables using the default data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param vars array of variables
     * @return matrix with collected values
     */
    static DMatrix copy(Var... vars) {
        return copy(defaultMType(), vars);
    }

    /**
     * Copies data from a list of variables using the specified data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param type matrix storage type
     * @param vars array of variables
     * @return matrix with collected values
     */
    static DMatrix copy(MType type, Var... vars) {
        int rows = vars[0].size();
        int cols = vars.length;
        DMatrix m = empty(type, rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, vars[j].getDouble(i));
            }
        }
        return m;
    }

    static DMatrix copy(boolean byRows, DVector... vectors) {
        MType type = byRows ? MType.RDENSE : MType.CDENSE;
        return copy(type, byRows, vectors);
    }

    static DMatrix copy(MType type, boolean byRows, DVector... vectors) {
        // TODO: can be improved but it needs better operation on vectors: store values in an external array
        int vlen = Arrays.stream(vectors).mapToInt(DVector::size).min().orElse(0);
        if (vlen == 0) {
            throw new IllegalArgumentException("Minimum length of a vector is 0 which is invalid.");
        }
        DMatrix copy;
        if (byRows) {
            copy = DMatrix.empty(type, vectors.length, vlen);
            for (int i = 0; i < vectors.length; i++) {
                for (int j = 0; j < vlen; j++) {
                    copy.set(i, j, vectors[i].get(j));
                }
            }
        } else {
            copy = DMatrix.empty(type, vlen, vectors.length);
            for (int i = 0; i < vlen; i++) {
                for (int j = 0; j < vectors.length; j++) {
                    copy.set(i, j, vectors[j].get(i));
                }
            }
        }
        return copy;
    }

    /**
     * @return matrix storage type
     */
    MType type();

    /**
     * If the matrix is an indirect storage type this returns
     * the source matrix storage type. Otherwise, it returns the
     * same value as {@link #type()}.
     *
     * @return inner matrix storage type
     */
    MType innerType();

    /**
     * @return number of rows of the matrix
     */
    int rowCount();

    /**
     * @return number of columns of the matrix
     */
    int colCount();

    /**
     * Getter for value found at given row and column index.
     *
     * @param row row index
     * @param col column index
     * @return value at given row index and column index
     */
    double get(final int row, final int col);

    /**
     * Sets value at the given row and column indexes
     *
     * @param row   row index
     * @param col   column index
     * @param value value to be set
     */
    void set(final int row, int col, final double value);

    /**
     * Increment the value at given position.
     *
     * @param row   row index
     * @param col   column index
     * @param value value to be added
     */
    void inc(final int row, final int col, final double value);

    /**
     * Returns a vector build from values of the row with given index in the matrix.
     * <p>
     * Depending on implementation, the vector can be a view over the original data.
     * To enforce a new copy add option {@link Algebra#copy()} as parameter.
     *
     * @param index index of the selected row
     * @return result vector reference
     */
    DVector mapRow(final int index, AlgebraOption<?>... opts);

    /**
     * Returns a vector build from values of the column with given index in the matrix.
     * <p>
     * Depending on implementation, the vector can be a view over the original data.
     * To enforce a new copy add option {@link Algebra#copy()} as parameter.
     *
     * @param index index of the selected column
     * @return result vector reference
     */
    DVector mapCol(final int index, AlgebraOption<?>... opts);

    /**
     * Creates a new matrix which contains only the rows
     * specified by given indexes.
     * <p>
     * Depending on implementation, the vector can be a view over the original data.
     * To enforce a new copy add option {@link Algebra#copy()} as parameter.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapRows(int[] indexes, AlgebraOption<?>... opts);

    /**
     * Creates a new matrix which contains only the columns
     * specified by given indexes.
     * <p>
     * Depending on implementation, the vector can be a view over the original data.
     * To enforce a new copy add option {@link Algebra#copy()} as parameter.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapCols(int[] indexes, AlgebraOption<?>... opts);

    /**
     * Creates a new vector with the index value from each row (axis=0) or
     * column (axis=1). The length of the index array should match the number
     * of rows (axis=0) o columns (axis=1).
     *
     * @param indexes index for each element
     * @param axis 0 for rows, 1 for columns
     * @return vector with indexed values
     */
    DVector mapValues(int[] indexes, int axis);

    /**
     * Creates a new matrix which contains only rows with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive.
     * <p>
     * Depending on the implementation
     * the new matrix can be a view. To obtain a new matrix copy
     * one has to add {@link Algebra#copy()} parameter.
     *
     * @param start start row index (inclusive)
     * @param end   end row index (exclusive)
     * @return result matrix reference
     */
    DMatrix rangeRows(int start, int end, AlgebraOption<?>... opts);

    /**
     * Creates a new matrix which contains only columns with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive.
     * <p>
     * Depending on the implementation
     * the new matrix can be a view. To obtain a new matrix copy
     * one has to add {@link Algebra#copy()} parameter.
     *
     * @param start start col index (inclusive)
     * @param end   end col index (exclusive)
     * @return result matrix reference
     */
    DMatrix rangeCols(int start, int end, AlgebraOption<?>... opts);

    /**
     * Builds a new matrix having all rows not specified by given indexes.
     * <p>
     * Depending on the implementation this can be a view over the original matrix.
     * To obtain a new copy of the data method {@link Algebra#copy()} must be added as parameter.
     *
     * @param indexes rows to be removed
     * @return new mapped matrix containing all rows not specified by indexes
     */
    DMatrix removeRows(int[] indexes, AlgebraOption<?>... opts);

    /**
     * Builds a new matrix having all rows not specified by given indexes.
     * <p>
     * Depending on the implementation this can be a view over the original matrix.
     * To obtain a new copy of the data method {@link Algebra#copy()} must be added as parameter.
     *
     * @param indexes rows to be removed
     * @return new mapped matrix containing all rows not specified by indexes
     */
    DMatrix removeCols(int[] indexes, AlgebraOption<?>... opts);

    /**
     * Adds a scalar value to all elements of a matrix. If possible,
     * the operation is realized in place.
     *
     * @param x value to be added
     * @return instance of the result matrix
     */
    DMatrix add(double x, AlgebraOption<?>... opts);

    /**
     * Add vector values each row/column of the matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix add(DVector x, int axis, AlgebraOption<?>... opts);

    /**
     * Adds element wise values from given matrix. If possible,
     * the operation is realized in place.
     *
     * @param b matrix with elements to be added
     * @return instance of the result matrix
     */
    DMatrix add(DMatrix b, AlgebraOption<?>... opts);

    /**
     * Subtract a scalar value to all elements of a matrix. If possible,
     * the operation is realized in place.
     *
     * @param x value to be substracted
     * @return instance of the result matrix
     */
    DMatrix sub(double x, AlgebraOption<?>... opts);

    /**
     * Subtract vector values to all rows (axis 0) or vectors (axis 1).
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix sub(DVector x, int axis, AlgebraOption<?>... opts);

    /**
     * Subtracts element wise values from given matrix. If possible,
     * the operation is realized in place.
     *
     * @param b matrix with elements to be substracted
     * @return instance of the result matrix
     */
    DMatrix sub(DMatrix b, AlgebraOption<?>... opts);

    /**
     * Multiply a scalar value to all elements of a matrix. If possible,
     * the operation is realized in place.
     *
     * @param x value to be multiplied with
     * @return instance of the result matrix
     */
    DMatrix mul(double x, AlgebraOption<?>... opts);

    /**
     * Multiply vector values to all rows (axis 0) or columns (axis 1).
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix mul(DVector x, int axis, AlgebraOption<?>... opts);

    /**
     * Multiplies element wise values from given matrix. If possible,
     * the operation is realized in place.
     *
     * @param b matrix with elements to be multiplied with
     * @return instance of the result matrix
     */
    DMatrix mul(DMatrix b, AlgebraOption<?>... opts);

    /**
     * Divide a scalar value from all elements of a matrix. If possible,
     * the operation is realized in place.
     *
     * @param x divisor value
     * @return instance of the result matrix
     */
    DMatrix div(double x, AlgebraOption<?>... opts);

    /**
     * Divide all rows (axis 0) or columns (axis 1) by elements of the given vector
     *
     * @param x    vector to be added
     * @param axis axis addition
     * @return same matrix with added values
     */
    DMatrix div(DVector x, int axis, AlgebraOption<?>... opts);

    /**
     * Divides element wise values from given matrix. If possible,
     * the operation is realized in place.
     *
     * @param b matrix with division elements
     * @return instance of the result matrix
     */
    DMatrix div(DMatrix b, AlgebraOption<?>... opts);

    /**
     * Apply the given function to all elements of the matrix.
     *
     * @param fun function to be applied
     * @return same instance matrix
     */
    DMatrix apply(Double2DoubleFunction fun, AlgebraOption<?>... opts);

    /**
     * Computes matrix vector multiplication.
     *
     * @param b vector to be multiplied with
     * @return result vector
     */
    DVector dot(DVector b);

    /**
     * Computes matrix - matrix multiplication.
     *
     * @param b matrix to be multiplied with
     * @return matrix result
     */
    DMatrix dot(DMatrix b);

    /**
     * Trace of the matrix, if the matrix is square. The trace of a squared
     * matrix is the sum of the elements from the main diagonal.
     * Otherwise returns an exception.
     *
     * @return value of the matrix trace
     */
    double trace();

    /**
     * Matrix rank obtained using singular value decomposition.
     *
     * @return effective numerical rank, obtained from SVD.
     */
    int rank();

    /**
     * Creates an instance of a transposed matrix. Depending on implementation
     * this can be a view of the original data.
     *
     * @return new transposed matrix
     */
    DMatrix t(AlgebraOption<?>... opts);

    /**
     * Vector with values from main diagonal
     */
    DVector diag();

    /**
     * Computes scatter matrix.
     *
     * @return scatter matrix instance
     */
    DMatrix scatter();

    /**
     * Builds a vector with maximum values from rows/cols.
     * If axis = 0 and matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the maximum
     * value from the row with that position.
     *
     * @param axis axis for which to compute maximal values
     * @return vector with result values
     */
    DVector max(int axis);

    /**
     * Builds a vector with indexes of the maximum values from rows/columns.
     * Thus if a matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the maximum
     * value from the row with that position.
     *
     * @return vector with indexes of max value values
     */
    int[] argmax(int axis);

    /**
     * Builds a vector with minimum values from rows/cols.
     * If axis = 0 and matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the minimum
     * value from the row with that position.
     *
     * @param axis axis for which to compute maximal values
     * @return vector with result values
     */
    DVector min(int axis);

    /**
     * Builds a vector with indexes of the minimum value index from rows/columns.
     * Thus if a matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the minimum
     * value index from the row with that position.
     *
     * @return vector with indexes of max values
     */
    int[] argmin(int axis);

    /**
     * Computes the sum of all elements from the matrix.
     *
     * @return scalar value with sum
     */
    double sum();

    /**
     * Computes the sum of all elements on the given axis. If axis
     * is 0 it will compute sum on rows, the resulting vector having size
     * as the number of rows and on each position the sum of elements from
     * that row. If the axis is 1 it will compute sums on columns.
     *
     * @param axis specifies the dimension used for summing
     * @return vector of sums on the given axis
     */
    DVector sum(int axis);

    /**
     * Computes the mean of all elements of the matrix.
     *
     * @return mean of all matrix elements
     */
    default double mean() {
        return sum() / (rowCount() * colCount());
    }

    /**
     * Computes vector of means along the specified axis.
     *
     * @param axis 0 for rows,  for columns
     * @return vector of means along axis
     */
    default DVector mean(int axis) {
        return sum(axis).div(axis == 0 ? rowCount() : colCount());
    }

    /**
     * Compute the variance of all elements of the matrix.
     *
     * @return variance of all elements of the matrix
     */
    double variance();

    /**
     * Computes vector of variances along the given axis of the matrix.
     *
     * @param axis 0 for rows, 1 for columns
     * @return vector of variances computed along given axis
     */
    DVector variance(int axis);

    /**
     * Compute the standard deviation of all elements of the matrix.
     *
     * @return standard deviation of all elements of the matrix
     */
    default double sd() {
        return Math.sqrt(variance());
    }

    /**
     * Computes vector of standard deviations along the given axis of the matrix.
     *
     * @param axis 0 for rows, 1 for columns
     * @return vector of standard deviations computed along given axis
     */
    default DVector sd(int axis) {
        return variance(axis).apply(Math::sqrt);
    }

    /**
     * Stream of double values, the element order is not guaranteed,
     * it depends on the implementation.
     *
     * @return double value stream
     */
    DoubleStream valueStream();

    /**
     * Creates a copy of a matrix.
     *
     * @return copy matrix reference
     */
    DMatrix copy();

    /**
     * Compares matrices using a tolerance of 1e-12 for values.
     * If the absolute difference between two values is less
     * than the specified tolerance, than the values are
     * considered equal.
     *
     * @param m matrix to compare with
     * @return true if dimensions and elements are equal
     */
    default boolean deepEquals(DMatrix m) {
        return deepEquals(m, 1e-12);
    }

    /**
     * Compares matrices using a tolerance for values.
     * If the absolute difference between two values is less
     * than the specified tolerance, than the values are
     * considered equal.
     *
     * @param m   matrix to compare with
     * @param eps tolerance
     * @return true if dimensions and elements are equal
     */
    boolean deepEquals(DMatrix m, double eps);

    DMatrix resizeCopy(int rows, int cols, double fill);
}
