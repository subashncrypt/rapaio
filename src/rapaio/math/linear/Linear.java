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

import rapaio.math.linear.decomposition.EigenDecomposition;

/**
 * Linear algebra tool bag class.
 * Contains various utilities to create and manipulate linear algbra constructs like {@link DMatrix} or {@link DVector}
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 2/6/15.
 */
public final class Linear {

    private Linear() {
    }

    public static EigenPair eigenDecomp(DMatrix s) {

        int n = s.cols();
        EigenDecomposition evd = EigenDecomposition.from(s);

        double[] _values = evd.getRealEigenvalues();
        DMatrix _vectors = evd.getV();

        DVector values = DVector.zeros(n);
        DMatrix vectors = DMatrix.empty(n, n);

        for (int i = 0; i < values.size(); i++) {
            values.set(values.size() - i - 1, _values[i]);
        }
        for (int i = 0; i < vectors.rows(); i++) {
            for (int j = 0; j < vectors.cols(); j++) {
                vectors.set(i, vectors.cols() - j - 1, _vectors.get(i, j));
            }
        }
        return EigenPair.from(values, vectors);
    }

    public static DMatrix pdPower(DMatrix s, double power) {
        EigenPair eigenPair = eigenDecomp(s);
        DMatrix U = eigenPair.vectors();
        DMatrix lambda = eigenPair.expandedValues();
        for (int i = 0; i < lambda.rows(); i++) {
            //TODO quick fix
            // this is because negative numbers can be produced for small quantities
            lambda.set(i, i, Math.pow(Math.abs(lambda.get(i, i)), power));
        }
        return U.dot(lambda).dot(U.t());
    }

    @SuppressWarnings("unused")
    private static boolean inTolerance(DMatrix s, double tol) {
        for (int i = 0; i < s.rows(); i++) {
            for (int j = i + 1; j < s.cols(); j++) {
                if (Math.abs(s.get(i, j)) > tol)
                    return false;
            }
        }
        return true;
    }

}
