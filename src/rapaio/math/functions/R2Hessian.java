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

package rapaio.math.functions;

import java.io.Serial;
import java.util.function.BiFunction;

import rapaio.math.linear.DMatrix;
import rapaio.math.linear.DVector;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/27/17.
 */
public record R2Hessian(BiFunction<Double, Double, DMatrix> f) implements RHessian {

    @Serial
    private static final long serialVersionUID = -7499515114017044967L;

    @Override
    public DMatrix apply(double... x) {
        return f.apply(x[0], x[1]);
    }

    @Override
    public DMatrix apply(DVector x) {
        return f.apply(x.get(0), x.get(1));
    }
}
