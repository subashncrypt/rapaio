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

package rapaio.ml.model.simple;

import java.io.Serial;
import java.util.Objects;
import java.util.Random;

import rapaio.core.distributions.Distribution;
import rapaio.core.distributions.Uniform;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarType;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.param.ValueParam;
import rapaio.ml.model.RegressionModel;
import rapaio.ml.model.RegressionResult;
import rapaio.ml.model.RunInfo;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

/**
 * A trivial regression which predicts using random
 * values provided by a distribution.
 * <p>
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
public class RandomValueRegression extends RegressionModel<RandomValueRegression, RegressionResult, RunInfo<RandomValueRegression>> {

    @Serial
    private static final long serialVersionUID = 819192240406617594L;

    public static RandomValueRegression newRVR() {
        return new RandomValueRegression();
    }

    public static RandomValueRegression from(Distribution distribution) {
        return new RandomValueRegression().distribution.set(distribution);
    }

    /**
     * Distribution used to generate prediction
     */
    public final ValueParam<Distribution, RandomValueRegression> distribution =
            new ValueParam<>(this, Uniform.of(0, 1), "distribution", Objects::nonNull);

    @Override
    public RandomValueRegression newInstance() {
        return new RandomValueRegression().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "RandomValueRegression";
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .inputs(0, 1_000_000, true, VarType.DOUBLE, VarType.BINARY, VarType.INT, VarType.NOMINAL, VarType.LONG, VarType.STRING)
                .targets(1, 1, true, VarType.DOUBLE);
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        return true;
    }

    @Override
    protected RegressionResult corePredict(final Frame df, final boolean withResiduals, final double[] quantiles) {
        RegressionResult pred = RegressionResult.build(this, df, withResiduals, quantiles);
        Random random = getRandom();
        for (String targetName : targetNames()) {
            pred.prediction(targetName).stream().forEach(s -> s.setDouble(distribution.get().sampleNext(random)));
        }
        pred.buildComplete();
        return pred;
    }

    @Override
    public String toString() {
        return fullName();
    }

    @Override
    public String toContent(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(headerSummary());
        if (isFitted()) {
            sb.append("Model is trained.\n");
        }
        return sb.toString();
    }

    @Override
    public String toFullContent(Printer printer, POption<?>... options) {
        return toContent(printer, options);
    }

    @Override
    public String toSummary(Printer printer, POption<?>... options) {
        return toContent(printer, options);
    }
}
