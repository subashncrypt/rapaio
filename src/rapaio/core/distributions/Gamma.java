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

import static rapaio.math.MathTools.*;
import static rapaio.printer.Format.*;

import java.io.Serial;
import java.util.Random;

import rapaio.printer.Format;

/**
 * Gamma distribution;
 * <A HREF="http://wwwinfo.cern.ch/asdoc/shortwrupsdir/g106/top.html"\n > math definition</A>,
 * <A HREF="http://www.cern.ch/RD11/rkb/AN16pp/node96.html#SECTION000960000000000000000"\n > definition of gamma function</A>
 * and
 * <A HREF="http://www.statsoft.com/textbook/glosf.html#Gamma Distribution">animated definition</A>.
 * <p>
 * <tt>p(x) = k * x^(alpha-1) * e^(-x/beta)</tt> with
 * <tt>k = 1/(g(alpha) * b^a))</tt> and <tt>g(a)</tt> being the gamma function.
 * <p>
 * Valid parameter ranges: <tt>alpha &gt; 0</tt>.
 * <p>
 * Note: For a Gamma distribution to have the mean <tt>mean</tt> and variance
 * <tt>variance</tt>, set the parameters as follows:
 * <p>
 * <pre>
 * alpha = mean * mean / variance;
 * lambda = 1 / (variance / mean);
 * </pre>
 * <p>
 * <p>
 * Instance methods operate on a user supplied uniform random number generator;
 * they are unsynchronized.
 * <dt>Static methods operate on a default uniform random number generator; they
 * are synchronized.
 * <p>
 * <b>Implementation:</b>
 * <dt>Method: Acceptance Rejection combined with Acceptance Complement.
 * <dt>High performance implementation. This is a port of
 * <A HREF="http://wwwinfo.cern.ch/asd/lhc++/clhep/manual/RefGuide/Random/RandGamma.html">RandGamma</A>
 * used in
 * <A HREF="http://wwwinfo.cern.ch/asd/lhc++/clhep">CLHEP 1.4.0</A>
 * (c++). CLHEP's implementation, in turn, is based on <tt>gds.c</tt>
 * from the
 * <A HREF="http://www.cis.tu-graz.ac.at/stat/stadl/random.html">c-RAND / WIN-RAND</A>
 * library. c-RAND's implementation, in turn, is based upon
 * <p>
 * J.H. Ahrens, U. Dieter (1974): Computer methods for sampling from gamma,
 * beta, Poisson and binomial distributions, Computing 12, 223-246.
 * and J.H. Ahrens, U. Dieter (1982): Generating gamma variates by a modified
 * rejection technique, Communications of the ACM 25, 47-54.
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
public record Gamma(double alpha, double beta) implements Distribution {

    public static Gamma of(double alpha, double beta) {
        return new Gamma(alpha, beta);
    }

    @Serial
    private static final long serialVersionUID = -7748384822665249829L;

    /**
     * Constructs a Gamma distribution. Example: alpha=1.0, beta=1.0.
     *
     * @throws IllegalArgumentException if <tt>alpha <= 0.0 || beta <= 0.0</tt>.
     */
    public Gamma {
        if (alpha <= 0 || beta <= 0) {
            throw new IllegalArgumentException("Value parameters alpha (" + Format.floatFlex(alpha) +
                    ") and beta (" + Format.floatFlex(beta) + ") parameters should be strictly positive.");
        }
    }

    @Override
    public String name() {
        return "Gamma(alpha=" + floatFlex(alpha) + ", beta=" + floatFlex(beta) + ")";
    }

    @Override
    public boolean discrete() {
        return false;
    }

    /**
     * Returns the probability distribution function.
     */
    public double pdf(double x) {
        if (x < 0) {
            return Double.NaN;
        }
        if (x == 0) {
            if (alpha == 1.0) {
                return 1.0 / beta;
            }
            if (alpha < 1.0) {
                return Double.POSITIVE_INFINITY;
            }
            return 0.0;
        }
        if (alpha == 1.0) {
            return exp(-x / beta) / beta;
        }
        return exp((alpha - 1.0) * log(x / beta) - x / beta - lnGamma(alpha)) / beta;
    }

    /**
     * Returns the cumulative distribution function.
     */
    public double cdf(double x) {
        if (x < 0.0) {
            return 0.0;
        }
        return incGamma(alpha, x / beta);
    }

    @Override
    public double quantile(double p) {
        if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }

        double cdf0 = cdf(0);
        if (p <= cdf0) {
            return 0;
        }

        // unbounded binary search
        double low = 0;
        double up = 1;

        // double up until we found a bound
        double cdf_up = cdf(up);
        while (cdf_up <= p) {
            up *= 2;
            cdf_up = cdf(up);
        }
        while (true) {
            double mid = (low + up) / 2;
            double cdf_mid = cdf(mid);
            double err = abs(up - low);
            if (err <= 1e-20) {
                return up;
            }
            if (cdf_mid < p) {
                if (low >= mid) {
                    return up;
                }
                low = mid;
            } else {
                if (up <= mid) {
                    return up;
                }
                up = mid;
            }
        }
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double sampleNext() {
        return sampleNext(new Random());
    }

    @Override
    public double sampleNext(final Random random) {
        /* **********************************************************************
         * * Gamma Distribution - Acceptance Rejection combined with Acceptance Complement * *
         * ***************************************************************** *
         * FUNCTION: - gds samples a random number from the standard
         * gamma distribution with parameter a > 0.
         * Acceptance Rejection gs for a < 1 ,
         * Acceptance Complement gd for a >= 1 .
         * REFERENCES: - J.H. Ahrens, U. Dieter (1974): Computer methods for sampling from gamma,
         * beta, Poisson and binomial distributions, Computing 12, 223-246. *
         * - J.H. Ahrens, U. Dieter (1982): Generating gamma variates by a
         * modified rejection technique, Communications of the ACM 25, 47-54.
         * SUBPROGRAMS: - drand(seed) ... (0,1)-Uniform generator with
         * unsigned long integer *seed * - NORMAL(seed) ... Normal generator
         * N(0,1). * *
         **********************************************************************/
        double a = alpha;
        double beta1 = 1 / beta;
        double aaa = -1.0, b = 0.0, c = 0.0, d, e, r, s, si = 0.0, ss;
        double q0 = 0.0;
        double q1 = 0.0416666664;
        double q2 = 0.0208333723;
        double q3 = 0.0079849875;
        double q4 = 0.0015746717;
        double q5 = -0.0003349403;
        double q6 = 0.0003340332;
        double q7 = 0.0006053049;
        double q8 = -0.0004701849;
        double q9 = 0.0001710320;
        double a1 = 0.333333333;
        double a2 = -0.249999949;
        double a3 = 0.19999986;
        double a4 = -0.166677482;
        double a5 = 0.142873973;
        double a6 = -0.124385581;
        double a7 = 0.110368310;
        double a8 = -0.112750886;
        double a9 = 0.104089866;
        double e1 = 1.000000000;
        double e2 = 0.499999994;
        double e3 = 0.166666848;
        double e4 = 0.041664508;
        double e5 = 0.008345522;
        double e6 = 0.001353826;
        double e7 = 0.000247453;

        double gds, p, q, t, sign_u, u, v, w, x;
        double v1, v2, v12;

        // Check for invalid input values

        if (a < 1.0) {
            // CASE A: Acceptance rejection algorithm gs
            b = 1.0 + 0.36788794412 * a;
            // Step 1
            for (; ; ) {
                p = b * random.nextDouble();
                if (p <= 1.0) {
                    // Step 2. Case gds <= 1
                    gds = exp(Math.log(p) / a);
                    if (log(random.nextDouble()) <= -gds) {
                        return (gds / beta1);
                    }
                } else {
                    // Step 3. Case gds > 1
                    gds = -log((b - p) / a);
                    if (log(random.nextDouble()) <= ((a - 1.0) * log(gds))) {
                        return (gds / beta1);
                    }
                }
            }
        } else {
            // CASE B: Acceptance complement algorithm gd (gaussian distribution, box muller transformation)
            // Step 1. Preparations
            ss = a - 0.5;
            s = sqrt(ss);
            d = 5.656854249 - 12.0 * s;
            // Step 2. Normal deviate
            do {
                v1 = 2.0 * random.nextDouble() - 1.0;
                v2 = 2.0 * random.nextDouble() - 1.0;
                v12 = v1 * v1 + v2 * v2;
            } while (v12 > 1.0);
            t = v1 * sqrt(-2.0 * log(v12) / v12);
            x = s + 0.5 * t;
            gds = x * x;
            if (t >= 0.0) {
                return (gds / beta1); // Immediate acceptance
            }

            // Step 3. Uniform random number
            u = random.nextDouble();
            if (d * u <= t * t * t) {
                return (gds / beta1); // Squeeze acceptance
            }

            // Step 4. Set-up for hat case
            if (a != aaa) {
                r = 1.0 / a;
                q0 = ((((((((q9 * r + q8) * r + q7) * r + q6) * r + q5) * r + q4) * r + q3) * r + q2) * r + q1) * r;
                if (a > 3.686) {
                    if (a > 13.022) {
                        b = 1.77;
                        si = 0.75;
                        c = 0.1515 / s;
                    } else {
                        b = 1.654 + 0.0076 * ss;
                        si = 1.68 / s + 0.275;
                        c = 0.062 / s + 0.024;
                    }
                } else {
                    b = 0.463 + s - 0.178 * ss;
                    si = 1.235;
                    c = 0.195 / s - 0.079 + 0.016 * s;
                }
            }
            if (x > 0.0) { // Step 5. Calculation of q
                v = t / (s + s); // Step 6.
                if (abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * log(1.0 + v);
                } else {
                    q = q0
                            + 0.5
                            * t
                            * t
                            * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1)
                            * v;
                } // Step 7. Quotient acceptance
                if (log(1.0 - u) <= q) {
                    return (gds / beta1);
                }
            }

            for (; ; ) { // Step 8. Double exponential deviate t
                do {
                    e = -log(random.nextDouble());
                    u = random.nextDouble();
                    u = u + u - 1.0;
                    sign_u = (u > 0) ? 1.0 : -1.0;
                    t = b + (e * si) * sign_u;
                } while (t <= -0.71874483771719); // Step 9. Rejection of t
                v = t / (s + s); // Step 10. New q(t)
                if (abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * log(1.0 + v);
                } else {
                    q = q0
                            + 0.5
                            * t
                            * t
                            * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1)
                            * v;
                }
                if (q <= 0.0) {
                    continue; // Step 11.
                }
                if (q > 0.5) {
                    w = exp(q) - 1.0;
                } else {
                    w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) * q + e1) * q;
                } // Step 12. Hat acceptance
                if (c * u * sign_u <= w * exp(e - 0.5 * t * t)) {
                    x = s + 0.5 * t;
                    return (x * x / beta1);
                }
            }
        }
    }

    @Override
    public double mean() {
        return alpha / beta;
    }

    @Override
    public double mode() {
        return Double.NaN;
    }

    @Override
    public double var() {
        return alpha / (beta * beta);
    }

    @Override
    public double skewness() {
        return 2 / sqrt(beta);
    }

    @Override
    public double kurtosis() {
        return 6 / alpha;
    }

    @Override
    public double entropy() {
        throw new IllegalArgumentException("Not implemented");
    }
}
