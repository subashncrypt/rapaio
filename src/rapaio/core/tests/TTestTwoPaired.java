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

package rapaio.core.tests;

import static rapaio.printer.Format.floatFlex;

import rapaio.core.distributions.StudentT;
import rapaio.core.stat.Mean;
import rapaio.core.stat.Variance;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

/**
 * t test two paired samples for mean of differences
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 7/8/16.
 */
public class TTestTwoPaired implements HTest {
    /**
     * Two paired samples t test for mean of the difference with default values
     * for significance level (0.05) and alternative (two tails)
     *
     * @param x    first given sample
     * @param y    second given sample
     * @param mean null hypothesis mean
     * @return an object containing hypothesis testing analysis
     */
    public static TTestTwoPaired test(Var x, Var y, double mean) {
        return new TTestTwoPaired(x, y, mean, 0.05, HTest.Alternative.TWO_TAILS);
    }

    /**
     * Two paired samples t test for mean of the differences
     *
     * @param x    first given sample
     * @param y    second given sample
     * @param mean null hypothesis mean
     * @param sl   significance level (usual value 0.05)
     * @param alt  alternative hypothesis (usual value two tails)
     * @return an object containing hypothesis testing analysis
     */
    public static TTestTwoPaired test(Var x, Var y, double mean, double sl, HTest.Alternative alt) {
        return new TTestTwoPaired(x, y, mean, sl, alt);
    }


    // parameters

    private final double mu;
    private final double sl;
    private final HTest.Alternative alt;

    // computed

    private final double sd;
    private final Var complete;
    private final double sampleMean;
    private final double df;
    private final double t;
    private final double pValue;
    private final double ciLow;
    private final double ciHigh;

    private TTestTwoPaired(Var x, Var y, double mu, double sl, HTest.Alternative alt) {
        this.mu = mu;
        this.sl = sl;
        this.alt = alt;

        complete = VarDouble.empty();

        for (int i = 0; i < Math.min(x.size(), y.size()); i++) {
            if (x.isMissing(i) || y.isMissing(i))
                continue;
            complete.addDouble(x.getDouble(i) - y.getDouble(i));
        }

        df = complete.size() - 1;

        if (complete.size() < 1) {
            // nothing to do
            sampleMean = Double.NaN;
            sd = Double.NaN;
            t = Double.NaN;
            pValue = Double.NaN;
            ciLow = Double.NaN;
            ciHigh = Double.NaN;

            return;
        }

        sampleMean = Mean.of(complete).value();
        sd = Variance.of(complete).sdValue();

        double sv = sd / Math.sqrt(complete.size());

        t = (sampleMean - mu) / sv;

        StudentT st = StudentT.of(df);
        switch (alt) {
            case GREATER_THAN -> pValue = 1 - st.cdf(t);
            case LESS_THAN -> pValue = st.cdf(t);
            default -> pValue = st.cdf(-Math.abs(t)) * 2;
        }

        ciLow = StudentT.of(df, sampleMean, sv).quantile(sl / 2);
        ciHigh = StudentT.of(df, sampleMean, sv).quantile(1 - sl / 2);
    }

    public double getMu() {
        return mu;
    }

    public double getSd() {
        return sd;
    }

    public double getDegrees() {
        return df;
    }

    public double getSl() {
        return sl;
    }

    public HTest.Alternative getAlt() {
        return alt;
    }

    public double getSampleMean() {
        return sampleMean;
    }

    public double getT() {
        return t;
    }

    public double pValue() {
        return pValue;
    }

    public double ciLow() {
        return ciLow;
    }

    public double ciHigh() {
        return ciHigh;
    }

    @Override
    public String toSummary(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("> TTestTwoPaired\n");
        sb.append("\n");
        sb.append(" Two Paired z-test\n");
        sb.append("\n");
        sb.append("complete rows: ").append(complete.size()).append("\n");
        sb.append("mean: ").append(floatFlex(mu)).append("\n");
        sb.append("significance level: ").append(floatFlex(sl)).append("\n");
        sb.append("alternative hypothesis: ").append(alt == HTest.Alternative.TWO_TAILS ? "two tails " : "one tail ").append(alt.pCondition()).append("\n");
        sb.append("\n");
        sb.append("sample mean: ").append(floatFlex(sampleMean)).append("\n");
        sb.append("sample sd: ").append(floatFlex(sd)).append("\n");
        sb.append("df: ").append(df).append("\n");
        sb.append("t: ").append(floatFlex(t)).append("\n");
        sb.append("p-value: ").append(pValue).append("\n");

        sb.append("conf int: [").append(floatFlex(ciLow)).append(",").append(floatFlex(ciHigh)).append("]\n");

        return sb.toString();
    }
}
