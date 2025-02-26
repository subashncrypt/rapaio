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

package rapaio.math.optimization;

import static java.lang.Math.abs;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import rapaio.data.VarDouble;
import rapaio.math.functions.RDerivative;
import rapaio.math.functions.RFunction;
import rapaio.math.linear.DVector;
import rapaio.math.optimization.linesearch.BacktrackLineSearch;
import rapaio.math.optimization.linesearch.LineSearch;
import rapaio.ml.common.param.ParamSet;
import rapaio.ml.common.param.ValueParam;

/**
 * Implements the gradient descend optimization algorithm. Gradient descent is an optimization
 * algorithm which finds local minimum of a function using the gradient of the function.
 * Since this is a minimization algorithm, with each iteration it advances in the direction
 * of negative gradient to improve the function.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/18/17.
 */
public class SteepestDescentSolver extends ParamSet<SteepestDescentSolver> implements Solver {

    public static SteepestDescentSolver newSolver() {
        return new SteepestDescentSolver();
    }

    @Serial
    private static final long serialVersionUID = 6935528214774334177L;

    /**
     * Tolerance error admissible for accepting a convergent solution.
     */
    public final ValueParam<Double, SteepestDescentSolver> tol = new ValueParam<>(this, 1e-10, "tol");

    /**
     * Maximum number of iterations.
     */
    public final ValueParam<Integer, SteepestDescentSolver> maxIt = new ValueParam<>(this, 100_000, "maxIt");

    /**
     * Line search algorithm
     */
    public final ValueParam<LineSearch, SteepestDescentSolver> lineSearch =
            new ValueParam<>(this, BacktrackLineSearch.newSearch(), "lineSearch");

    /**
     * Function to be optimized.
     */
    public final ValueParam<RFunction, SteepestDescentSolver> f = new ValueParam<>(this, null, "f");

    /**
     * Function's derivative
     */
    public final ValueParam<RDerivative, SteepestDescentSolver> d1f = new ValueParam<>(this,null, "d1f");

    /**
     * Initial value
     */
    public final ValueParam<DVector, SteepestDescentSolver> x0 = new ValueParam<>(this,null, "x0");

    private DVector sol;

    private List<DVector> solutions;
    private VarDouble errors;
    private boolean converged = false;

    @Override
    public SteepestDescentSolver compute() {
        converged = false;
        errors = VarDouble.empty().name("errors");
        solutions = new ArrayList<>();
        sol = x0.get().copy();
        solutions.add(sol.copy());
        for (int i = 0; i < maxIt.get(); i++) {
            DVector p = d1f.get().apply(sol).mul(-1);
            double error = p.norm(2);
            errors.addDouble(error);
            if (abs(error) < tol.get()) {
                converged = true;
                break;
            }
            double t = lineSearch.get().search(f.get(), d1f.get(), sol, p);
            sol = sol.copy().add(p.mul(t));
            solutions.add(sol.copy());
        }
        return this;
    }

    @Override
    public String toString() {
        return "solution: " + sol.toString() + ","
                + "converged: " + converged + ","
                + "iterations: " + solutions.size();
    }

    @Override
    public List<DVector> solutions() {
        return solutions;
    }

    @Override
    public DVector solution() {
        return sol;
    }

    @Override
    public VarDouble errors() {
        return errors;
    }

    @Override
    public boolean hasConverged() {
        return converged;
    }
}
