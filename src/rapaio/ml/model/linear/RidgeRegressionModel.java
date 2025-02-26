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

package rapaio.ml.model.linear;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.preprocessing.AddIntercept;
import rapaio.math.linear.DMatrix;
import rapaio.ml.common.param.ValueParam;
import rapaio.ml.model.linear.impl.BaseLinearRegressionModel;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 7/26/20.
 */
public class RidgeRegressionModel extends BaseLinearRegressionModel<RidgeRegressionModel> {

    public static RidgeRegressionModel newModel(double lambda) {
        return newModel(lambda, Centering.MEAN, Scaling.SD);
    }

    public static RidgeRegressionModel newModel(double lambda, Centering centering, Scaling scaling) {
        return new RidgeRegressionModel()
                .lambda.set(lambda)
                .centering.set(centering)
                .scaling.set(scaling);
    }

    @Serial
    private static final long serialVersionUID = 6868244273014714128L;

    /**
     * Coefficient of the ridge penalty term (L2 regularization).
     */
    public final ValueParam<Double, RidgeRegressionModel> lambda = new ValueParam<>(this, 1.0, "lambda", Double::isFinite);

    /**
     * Type of variable centering
     */
    public final ValueParam<Centering, RidgeRegressionModel> centering =
            new ValueParam<>(this, Centering.MEAN, "centering", Objects::nonNull);

    /**
     * Type of variable scaling
     */
    public final ValueParam<Scaling, RidgeRegressionModel> scaling = new ValueParam<>(this, Scaling.SD, "scaling", Objects::nonNull);

    private Map<String, Double> inputMean;
    private Map<String, Double> inputScale;
    private Map<String, Double> targetMean;
    private Map<String, Double> targetScale;

    @Override
    public RidgeRegressionModel newInstance() {
        return new RidgeRegressionModel().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "RidgeRegression";
    }

    @Override
    protected FitSetup prepareFit(Frame df, Var weights, String... targetVarNames) {
        // add intercept variable
        Frame transformed = intercept.get() ? AddIntercept.transform().fapply(df) : df;

        // collect standard information
        FitSetup fitSetup = super.prepareFit(transformed, weights, targetVarNames);

        inputMean = new HashMap<>();
        inputScale = new HashMap<>();
        targetMean = new HashMap<>();
        targetScale = new HashMap<>();

        for (String inputName : inputNames) {
            if (AddIntercept.INTERCEPT.equals(inputName)) {
                inputMean.put(inputName, 0.0);
                inputScale.put(inputName, 1.0);
            } else {
                inputMean.put(inputName, centering.get().compute(fitSetup.df.rvar(inputName)));
                inputScale.put(inputName, scaling.get().compute(fitSetup.df.rvar(inputName)));
            }
        }
        for (String targetName : targetNames) {
            targetMean.put(targetName, centering.get().compute(fitSetup.df.rvar(targetName)));
            targetScale.put(targetName, scaling.get().compute(fitSetup.df.rvar(targetName)));
        }

        return FitSetup.valueOf(fitSetup.df, fitSetup.w, fitSetup.targetVars);
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        int interceptIndex = -1;
        String[] selNames = new String[inputNames.length - (intercept.get() ? 1 : 0)];
        int pos = 0;
        for (int i = 0; i < inputNames.length; i++) {
            if (AddIntercept.INTERCEPT.equals(inputNames[i])) {
                interceptIndex = i;
                continue;
            }
            selNames[pos++] = inputNames[i];
        }

        DMatrix X = DMatrix.empty(df.rowCount(), selNames.length);
        DMatrix Y = DMatrix.empty(df.rowCount(), targetNames.length);

        if (intercept.get()) {
            // scale in values if we have intercept
            for (int j = 0; j < selNames.length; j++) {
                int varIndex = df.varIndex(selNames[j]);
                double mean = inputMean.get(selNames[j]);
                double sd = inputScale.get(selNames[j]);
                for (int i = 0; i < df.rowCount(); i++) {
                    X.set(i, j, (df.getDouble(i, varIndex) - mean) / sd);
                }
            }
            for (int j = 0; j < targetNames.length; j++) {
                int varIndex = df.varIndex(targetNames[j]);
                double mean = targetMean.get(targetNames[j]);
                double sd = targetScale.get(targetNames[j]);
                for (int i = 0; i < df.rowCount(); i++) {
                    Y.set(i, j, (df.getDouble(i, varIndex) - mean) / sd);
                }
            }
        } else {
            // if we do not have intercept we ignore centering and scaling
            for (int j = 0; j < selNames.length; j++) {
                int varIndex = df.varIndex(selNames[j]);
                for (int i = 0; i < df.rowCount(); i++) {
                    X.set(i, j, df.getDouble(i, varIndex));
                }
            }
            for (int j = 0; j < targetNames.length; j++) {
                int varIndex = df.varIndex(targetNames[j]);
                for (int i = 0; i < df.rowCount(); i++) {
                    Y.set(i, j, df.getDouble(i, varIndex));
                }
            }
        }

        // solve the scaled system
        DMatrix l = DMatrix.eye(X.cols()).mul(lambda.get());
        DMatrix A = X.t().dot(X).add(l);
        DMatrix B = X.t().dot(Y);
        DMatrix scaledBeta = A.qr().solve(B);

        if (intercept.get()) {
            beta = DMatrix.fill(scaledBeta.rows() + 1, scaledBeta.cols(), 0);

            for (int i = 0; i < targetNames.length; i++) {
                String targetName = targetName(i);
                double targetScale = this.targetScale.get(targetName);
                for (int j = 0; j < inputNames.length; j++) {
                    if (AddIntercept.INTERCEPT.equals(inputNames[j])) {
                        double interceptValue = targetMean.get(targetName);
                        for (int k = 0; k < inputNames.length; k++) {
                            if (k == j) {
                                continue;
                            }
                            int offset = k >= interceptIndex ? 1 : 0;
                            interceptValue -= scaledBeta.get(k - offset, i) * targetScale * inputMean.get(inputNames[k]) / inputScale.get(
                                    inputNames[k]);
                        }
                        beta.set(j, i, interceptValue);
                    } else {
                        int offset = j >= interceptIndex ? 1 : 0;
                        beta.set(j, i, scaledBeta.get(j - offset, i) * targetScale / inputScale.get(inputNames[j]));
                    }
                }
            }
        } else {
            beta = scaledBeta;
        }
        return true;
    }

}
