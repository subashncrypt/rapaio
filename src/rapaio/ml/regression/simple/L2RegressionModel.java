/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
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

package rapaio.ml.regression.simple;

import rapaio.core.stat.Mean;
import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.VType;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarNominal;
import rapaio.ml.common.Capabilities;
import rapaio.ml.regression.AbstractRegressionModel;
import rapaio.ml.regression.RegressionResult;
import rapaio.printer.Format;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

import java.util.Arrays;

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
public class L2RegressionModel extends AbstractRegressionModel<L2RegressionModel, RegressionResult> {

    private static final long serialVersionUID = -8666168876139028337L;

    public static L2RegressionModel newModel() {
        return new L2RegressionModel();
    }

    private double[] means;

    private L2RegressionModel() {
    }

    @Override
    public L2RegressionModel newInstance() {
        return new L2RegressionModel();
    }

    @Override
    public String name() {
        return "L2Regression";
    }

    @Override
    public Capabilities capabilities() {
        return Capabilities.builder()
                .minInputCount(0).maxInputCount(1_000_000)
                .minTargetCount(1).maxTargetCount(1_000_000)
                .inputTypes(Arrays.asList(VType.DOUBLE, VType.BINARY, VType.INT, VType.NOMINAL, VType.LONG, VType.STRING))
                .targetType(VType.DOUBLE)
                .allowMissingInputValues(true)
                .allowMissingTargetValues(true)
                .build();
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        means = new double[targetNames().length];
        for (int i = 0; i < targetNames().length; i++) {
            double mean = Mean.of(df.rvar(targetName(i))).value();
            means[i] = mean;
        }
        return true;
    }

    @Override
    protected RegressionResult corePredict(final Frame df, final boolean withResiduals) {
        RegressionResult fit = RegressionResult.build(this, df, withResiduals);
        for (int i = 0; i < targetNames().length; i++) {
            double mean = means[i];
            Var v = fit.prediction(targetName(i));
            v.stream().forEach(s -> s.setDouble(mean));
        }
        fit.buildComplete();
        return fit;
    }

    public double[] getMeans() {
        return means;
    }

    @Override
    public String toString() {
        if (isFitted()) {
            StringBuilder sb = new StringBuilder();
            sb.append(fullName());
            sb.append("; fitted values={");
            for (int i = 0; i < Math.min(5, targetNames.length); i++) {
                sb.append(targetName(i)).append(":").append(Format.floatFlex(means[i]));
                if (i < targetNames.length - 1) {
                    sb.append(",");
                }
            }
            if (targetNames.length > 5) {
                sb.append("...");
            }
            sb.append("}");
            return sb.toString();
        }
        return fullName() + "; not fitted";
    }

    @Override
    public String toContent(Printer printer, POption... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(headerSummary());
        sb.append("\n");

        if (isFitted()) {
            sb.append("Fitted values:\n");
            sb.append("\n");

            Var target = VarNominal.empty().withName("Target");
            Var median = VarDouble.empty().withName("Fitted value");
            for (int i = 0; i < means.length; i++) {
                target.addLabel(targetName(i));
                median.addDouble(means[i]);
            }
            sb.append(SolidFrame.byVars(target, median).toContent(printer, options));
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toFullContent(Printer printer, POption... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(headerSummary());
        sb.append("\n");

        if (isFitted()) {
            sb.append("Fitted values:\n");
            sb.append("\n");

            Var target = VarNominal.empty().withName("Target");
            Var median = VarDouble.empty().withName("Fitted value");
            for (int i = 0; i < means.length; i++) {
                target.addLabel(targetName(i));
                median.addDouble(means[i]);
            }
            sb.append(SolidFrame.byVars(target, median).toFullContent(printer, options));
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toSummary(Printer printer, POption... options) {
        return toContent(printer, options);
    }
}
