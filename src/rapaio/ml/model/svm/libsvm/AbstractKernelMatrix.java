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

package rapaio.ml.model.svm.libsvm;

import java.util.Arrays;

import rapaio.math.linear.DVector;
import rapaio.math.linear.dense.DVectorDense;
import rapaio.ml.common.kernel.Kernel;
import rapaio.util.collection.TArrays;

public abstract class AbstractKernelMatrix {

    protected final DVector[] xs;
    protected final Kernel kernel;
    protected final Cache cache;
    protected final double[] qd;

    AbstractKernelMatrix(DVector[] xs, Kernel kernel, Cache cache, double[] qd) {
        this.kernel = kernel;
        this.xs = Arrays.copyOf(xs, xs.length);
        this.cache = cache;
        this.qd = qd;
    }

    abstract double[] getQD();

    abstract DVectorDense getQ(int column, int len);

    void swapIndex(int i, int j) {
        TArrays.swap(xs, i, j);
    }
}
