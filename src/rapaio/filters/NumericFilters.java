/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package rapaio.filters;

import rapaio.core.UnivariateFunction;
import rapaio.core.stat.Mean;
import rapaio.data.Frame;
import rapaio.data.NumericVector;
import rapaio.data.Vector;
import rapaio.distributions.Normal;

import static rapaio.core.BaseMath.pow;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides filter operations on numeric vectors.
 * <p/>
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public final class NumericFilters {
    private NumericFilters() {
    }

    /**
     * Compute the result of a numerical univariate function
     * using numerical values from a {@link Vector} as input parameter.
     *
     * @param vector input value vector
     * @param f      univariate function
     * @return function {@param f} applied over the values from {@param vector}
     */
    public static Vector applyFunction(Vector vector, UnivariateFunction f) {
        Vector ret = new NumericVector(vector.getName(), vector.getRowCount());
        for (int i = 0; i < vector.getRowCount(); i++) {
            if (vector.isMissing(i)) {
                continue;
            }
            ret.setValue(i, f.eval(vector.getValue(i)));
        }
        return ret;
    }

    /**
     * Alter valid numeric values with normally distributed noise.
     * <p/>
     * Noise comes from a normal distribution with
     * mean 0 and standard deviation 0.1
     *
     * @param vector input values
     * @return altered values
     */
    public static Vector jitter(Vector vector) {
        return jitter(vector, 0.1);
    }

    /**
     * Alter valid numeric values with normally distributed noise.
     * <p/>
     * Noise comes from a normal distribution with
     * mean 0 and standard deviation specified by {@param sd}
     *
     * @param vector input values
     * @param sd     standard deviation of the normally distributed noise
     * @return altered values
     */
    public static Vector jitter(Vector vector, double sd) {
        Normal d = new Normal(0, sd);
        Vector result = new NumericVector(vector.getName(), vector.getRowCount());
        Vector jitter = d.sample(result.getRowCount());
        for (int i = 0; i < result.getRowCount(); i++) {
            if (vector.isMissing(i)) {
                continue;
            }
            result.setValue(i, vector.getValue(i) + jitter.getValue(i));
        }
        return result;
    }

    public static void imputeMissing(Frame df, String imputedName, String method, Vector... groupBy) {

        for (int i = 0; i < groupBy.length; i++) {
            if (!groupBy[i].isNominal()) {
                throw new IllegalArgumentException("impute works only with nominal groups");
            }
        }
        Vector imputed = df.getCol(imputedName);
        if (groupBy.length == 0) {
            double mean = new Mean(imputed).getValue();
            for (int i = 0; i < imputed.getRowCount(); i++) {
                if (imputed.isMissing(i)) {
                    imputed.setValue(i, mean);
                }
            }
        } else {
            List<Frame> frames = new ArrayList<>();
            frames.add(df);

            for (int i = 0; i < groupBy.length; i++) {
                List<Frame> splitted = new ArrayList<>();
                for (Frame frame : frames) {
                    Frame[] results = NominalFilters.groupByNominal(frame, df.getColIndex(groupBy[i].getName()));
                    for (int j = 0; j < results.length; j++) {
                        splitted.add(results[j]);
                    }
                }
                frames = splitted;
            }

            for (Frame frame : frames) {
                double mean = new Mean(frame.getCol(imputed.getName())).getValue();
                for (int i = 0; i < frame.getRowCount(); i++) {
                    if (frame.getCol(imputedName).isMissing(i)) {
                        frame.setValue(i, frame.getColIndex(imputedName), mean);
                    }
                }
            }
        }


    }

}
