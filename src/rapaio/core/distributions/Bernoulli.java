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

package rapaio.core.distributions;

import static java.lang.StrictMath.*;

import java.io.Serial;
import java.util.Random;

/**
 * Bernoulli distribution
 *
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public final class Bernoulli implements Distribution {

    public static Bernoulli of(double p) {
        return new Bernoulli(p);
    }

    @Serial
    private static final long serialVersionUID = -180129876504915848L;
    private final double prob;

    private Bernoulli(double p) {
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("Probability parameter must be in closed interval [0,1]");
        }
        this.prob = p;
    }

    @Override
    public boolean discrete() {
        return true;
    }

    @Override
    public String name() {
        return "Ber(p=" + prob + ")";
    }

    @Override
    public double pdf(double x) {
        if (x == 0) {
            return 1 - prob;
        }
        if (x == 1) {
            return prob;
        }
        return 0;
    }

    @Override
    public double cdf(double x) {
        if (x < 0) {
            return 0;
        }
        if (x < 1) {
            return 1 - prob;
        }
        return 1;
    }

    @Override
    public double quantile(double p) {
        return (p <= 1 - this.prob) ? 0 : 1;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public double mean() {
        return prob;
    }

    @Override
    public double mode() {
        return (prob < 0.5) ? 0 : 1;
    }

    @Override
    public double var() {
        return prob * (1 - prob);
    }

    @Override
    public double skewness() {
        return 1 / sqrt((1 - prob) * prob);
    }

    @Override
    public double kurtosis() {
        double prod = (1 - prob) * prob;
        return (1 - 6 * prod) / prod;
    }

    @Override
    public double entropy() {
        return -prob * log(prob) - (1 - prob) * log(1 - prob);
    }

    @Override
    public double sampleNext() {
        return new Random().nextDouble() <= prob ? 1 : 0;
    }

    @Override
    public double sampleNext(final Random random) {
        return random.nextDouble() <= prob ? 1 : 0;
    }
}
