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

package rapaio.experiment.math.optimization;

import java.io.Serial;
import java.io.Serializable;

import rapaio.math.linear.DVector;
import rapaio.util.Pair;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/24/15.
 */
@Deprecated
public interface Updater extends Serializable {
    Pair<DVector, Double> compute(DVector weightsOld, DVector gradient, double stepSize, int iter, double regParam);
}

/**
 * A simple updater for gradient descent *without* any regularization.
 * Uses a step-size decreasing with the square root of the number of iterations.
 */
@Deprecated
class SimpleUpdater implements Updater {
    @Serial
    private static final long serialVersionUID = -2067278844383126771L;

    public Pair<DVector, Double> compute(DVector weightsOld, DVector gradient, double stepSize, int iter, double regParam) {
        double thisIterStepSize = stepSize / Math.sqrt(iter);
        DVector brzWeights = weightsOld.copy();
        brzWeights.add(gradient.copy().mul(-thisIterStepSize));
        return Pair.from(brzWeights, 0.0);
    }
}

/**
 * Updater for L1 regularized problems.
 * R(w) = ||w||_1
 * Uses a step-size decreasing with the square root of the number of iterations.
 * Instead of subgradient of the regularizer, the proximal operator for the
 * L1 regularization is applied after the gradient step. This is known to
 * result in better sparsity of the intermediate solution.
 * <p>
 * The corresponding proximal operator for the L1 norm is the soft-thresholding
 * function. That is, each weight component is shrunk towards 0 by shrinkageVal.
 * <p>
 * If w >  shrinkageVal, set weight component to w-shrinkageVal.
 * If w < -shrinkageVal, set weight component to w+shrinkageVal.
 * If -shrinkageVal < w < shrinkageVal, set weight component to 0.
 * <p>
 * Equivalently, set weight component to signum(w) * max(0.0, abs(w) - shrinkageVal)
 */
@Deprecated
class L1Updater implements Updater {
    @Serial
    private static final long serialVersionUID = -581601380754106199L;

    public Pair<DVector, Double> compute(DVector weightsOld, DVector gradient, double stepSize, int iter, double regParam) {
        double thisIterStepSize = stepSize / Math.sqrt(iter);
        // Take gradient step
        DVector brzWeights = weightsOld.copy();
        brzWeights.add(gradient.copy().mul(-thisIterStepSize));
        // Apply proximal operator (soft thresholding)
        double shrinkageVal = regParam * thisIterStepSize;
        int i = 0;
        int len = brzWeights.size();
        while (i < len) {
            double wi = brzWeights.get(i);
            brzWeights.set(i, Math.signum(wi) * Math.max(0.0, Math.abs(wi) - shrinkageVal));
            i += 1;
        }

        return Pair.from(brzWeights, brzWeights.norm(1) * regParam);
    }
}

/**
 * Updater for L2 regularized problems.
 * R(w) = 1/2 ||w||^2
 * Uses a step-size decreasing with the square root of the number of iterations.
 */
@Deprecated
class SquaredL2Updater implements Updater {
    @Serial
    private static final long serialVersionUID = -9217486067545972690L;

    public Pair<DVector, Double> compute(DVector weightsOld, DVector gradient, double stepSize, int iter, double regParam) {
        // add up both updates from the gradient of the loss (= step) as well as
        // the gradient of the regularizer (= regParam * weightsOld)
        // w' = w - thisIterStepSize * (gradient + regParam * w)
        // w' = (1 - thisIterStepSize * regParam) * w - thisIterStepSize * gradient
        double thisIterStepSize = stepSize / Math.sqrt(iter);
        DVector brzWeights = weightsOld.copy();
        brzWeights.sub(brzWeights.copy().mul(thisIterStepSize * regParam));
        brzWeights.add(gradient.copy().mul(-thisIterStepSize));
        double norm = brzWeights.norm(2.0);

        return Pair.from(brzWeights, 0.5 * regParam * norm * norm);
    }
}