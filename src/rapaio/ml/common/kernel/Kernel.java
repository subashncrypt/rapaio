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

package rapaio.ml.common.kernel;

import java.io.Serializable;

import rapaio.data.Frame;
import rapaio.math.linear.DVector;

/**
 * Kernel function interface
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 1/16/15.
 */
public interface Kernel extends Serializable {

    Kernel newInstance();

    String name();

    boolean isLinear();

    void buildKernelCache(String[] varNames, Frame df);

    double compute(Frame df1, int row1, Frame df2, int row2);

    double compute(DVector v, DVector u);

    default void clean() {
    }
}
