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

package rapaio.ml.model.boost;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarRange;
import rapaio.data.VarType;
import rapaio.math.linear.DVector;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.param.ValueParam;
import rapaio.ml.loss.L2Loss;
import rapaio.ml.loss.Loss;
import rapaio.ml.model.RegressionModel;
import rapaio.ml.model.RegressionResult;
import rapaio.ml.model.RunInfo;
import rapaio.ml.model.simple.L2Regression;
import rapaio.ml.model.tree.RTree;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

/**
 * Gradient Boosting Tree
 * <p>
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
public class GBTRegressionModel extends RegressionModel<GBTRegressionModel, RegressionResult, RunInfo<GBTRegressionModel>> {

    public static GBTRegressionModel newModel() {
        return new GBTRegressionModel();
    }

    @Serial
    private static final long serialVersionUID = 4559540258922653130L;

    /**
     * Shrinkage regularization coefficient
     */
    public final ValueParam<Double, GBTRegressionModel> shrinkage = new ValueParam<>(this, 1.0,
            "shrinkage", x -> Double.isFinite(x) && x > 0 && x <= 1);

    /**
     * Loss function used
     */
    public final ValueParam<Loss, GBTRegressionModel> loss = new ValueParam<>(this, new L2Loss(), "loss", Objects::nonNull);

    /**
     * First starting model
     */
    public final ValueParam<RegressionModel<?, ?, ?>, GBTRegressionModel> initModel = new ValueParam<>(this, L2Regression.newModel(),
            "initModel", Objects::nonNull);

    /**
     * Tree weak lerner model
     */
    public final ValueParam<GBTRtree<? extends RegressionModel<?, ?, ?>, ? extends RegressionResult, ?>, GBTRegressionModel>
            model = new ValueParam<>(this, RTree.newCART().maxDepth.set(2).minCount.set(10), "nodeModel", Objects::nonNull);

    /**
     * Convergence threshold used to stop tree growing if the progress on loss function is less than specified
     */
    public final ValueParam<Double, GBTRegressionModel> eps = new ValueParam<>(this, 1e-10, "eps", Double::isFinite);

    private VarDouble fitValues;

    private List<GBTRtree<? extends RegressionModel<?, ?, ?>, ? extends RegressionResult, ?>> trees;

    @Override
    public GBTRegressionModel newInstance() {
        return new GBTRegressionModel().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "GBTRegression";
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .inputs(1, 1_000_000, true, VarType.BINARY, VarType.INT, VarType.DOUBLE, VarType.NOMINAL)
                .targets(1, 1, false, VarType.DOUBLE);
    }

    public VarDouble getFitValues() {
        return fitValues;
    }

    public List<GBTRtree<? extends RegressionModel<?, ?, ?>, ? extends RegressionResult, ?>> getTrees() {
        return trees;
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {

        trees = new ArrayList<>();

        Random random = getRandom();
        Var y = df.rvar(firstTargetName());
        Frame x = df.removeVars(VarRange.of(firstTargetName()));

        initModel.get().fit(df, weights, firstTargetName());
        fitValues = initModel.get().predict(df, false).firstPrediction().copy();

        for (int i = 1; i <= runs.get(); i++) {

            Var gradient = loss.get().gradient(y, fitValues).name("target");

            Frame xm = x.bindVars(gradient);
            var tree = (GBTRtree<? extends RegressionModel<?, ?, ?>, ? extends RegressionResult, ?>) model.get()
                    .newInstance();

            // frame sampling

            Mapping sampleRows = rowSampler.get().nextSample(random, xm, weights).mapping();
            Frame xmLearn = xm.mapRows(sampleRows);

            // build regions

            tree.fit(xmLearn, "target");

            // predict residuals

            tree.boostUpdate(xmLearn, y.mapRows(sampleRows), fitValues.mapRows(sampleRows), loss.get());

            // add next prediction to the predict values
            var pred = tree.predict(df, false).firstPrediction();
            VarDouble nextFit = fitValues.dvNew().fma(shrinkage.get(), pred.dv()).dv();

            double initScore = loss.get().errorScore(y, fitValues);
            double nextScore = loss.get().errorScore(y, nextFit);

            if (Math.abs(initScore - nextScore) < eps.get()) {
                break;
            }

            if (initScore > nextScore) {
                fitValues = nextFit;
                // add tree in the predictors list
                trees.add(tree);
            }
            runningHook.get().accept(RunInfo.forRegression(this, i));
        }
        return true;
    }

    @Override
    protected RegressionResult corePredict(final Frame df, final boolean withResiduals, double[] quantiles) {
        RegressionResult result = RegressionResult.build(this, df, withResiduals, quantiles);
        DVector prediction = result.firstPrediction().dv();

        prediction.apply(v -> 0);
        prediction.add(initModel.get().predict(df, false).firstPrediction().dv());
        for (var tree : trees) {
            prediction.fma(shrinkage.get(), tree.predict(df, false).firstPrediction().dv());
        }
        result.buildComplete();
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName()).append("; fitted=").append(isFitted());
        if (isFitted()) {
            sb.append(", fitted trees:").append(trees.size());
        }
        return sb.toString();
    }

    @Override
    public String toSummary(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(headerSummary());
        sb.append("\n");

        if (!hasLearned) {
            return sb.toString();
        }

        sb.append("Target <<< ").append(firstTargetName()).append(" >>>\n\n");
        sb.append("> Number of fitted trees: ").append(trees.size()).append("\n");

        return sb.toString();
    }

    @Override
    public String toContent(POption<?>... options) {
        return toSummary();
    }

    @Override
    public String toFullContent(POption<?>... options) {
        return toSummary();
    }
}
