/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2021 Aurelian Tutuianu
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
 *
 */

package rapaio.math.optimization.linesearch;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.math.functions.RDerivative;
import rapaio.math.functions.RFunction;
import rapaio.math.linear.DVector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 1/31/21.
 */
public class BacktrackLineSearchTest {

    @Test
    void validationTest() {
        assertThrows(IllegalArgumentException.class, () -> BacktrackLineSearch.newSearch().alpha.set(0.0));
    }

    @Test
    void smokeSquaredTest() {

        // here we test f(x) = x^2
        // after calculations for steepest descent alpha have a constant value
        RFunction f = (DVector x) -> x.get(0) * x.get(0);
        RDerivative df = (DVector x) -> x.copy().mult(2.0);

        for (int i = 0; i < 1_000; i++) {
            double next = (RandomSource.nextDouble() - 0.5) * 100;
            DVector x0 = DVector.wrap(next);
            DVector p = df.apply(x0).mult(-1);
            double t = BacktrackLineSearch.newSearch().search(f, df, x0, p);
            double fx0 = f.apply(x0);
            double fx1 = f.apply(p.caxpy(t, x0));
            assertTrue(fx0 >= fx1);
            assertEquals(0.7, t);
        }
    }

    @Test
    void smokeTest() {

        // here we test f(x) = -Math.exp(x^2)
        RFunction f = (DVector x) -> -Math.exp(-x.get(0) * x.get(0));
        RDerivative df = (DVector x) -> DVector.wrap(Math.exp(-x.get(0) * x.get(0)) * 2 * x.get(0));

        for (int i = 0; i < 1_000; i++) {
            double next = (RandomSource.nextDouble() - 0.5);
            DVector x0 = DVector.wrap(next);
            DVector p = df.apply(x0).mult(-1);
            double alpha = BacktrackLineSearch.newSearch().search(f, df, x0, p, 100_000.0);
            double fx0 = f.apply(x0);
            double fx1 = f.apply(p.caxpy(alpha, x0));
            assertTrue(fx0 >= fx1);
        }
    }

    @Test
    void documentedTests() {

        T[] tests = new T[]{
                new T(
                        // this test is taken from Algorithms for Optimization, p.58
                        v -> v.get(0) * v.get(0) + v.get(0) * v.get(1) + v.get(1) * v.get(1),
                        v -> DVector.wrap(2 * v.get(0), 2 * v.get(1)),
                        DVector.wrap(-1, -1),
                        DVector.wrap(1, 2),
                        1e-4,
                        0.5,
                        10,
                        DVector.wrap(-1.5, -0.5)
                )
        };

        for (T test : tests) {
            double alpha = BacktrackLineSearch.newSearch()
                    .alpha.set(test.alpha)
                    .beta.set(test.beta)
                    .search(test.f, test.d1f, test.x0, test.d, test.t);
            DVector x1 = test.x0.copy().add(test.d.mult(alpha));
            assertTrue(x1.deepEquals(test.x1));
        }

    }

    @RequiredArgsConstructor
    static class T {
        public final RFunction f;
        public final RDerivative d1f;
        public final DVector d;
        public final DVector x0;
        public final double alpha;
        public final double beta;
        public final double t;
        public final DVector x1;
    }
}
