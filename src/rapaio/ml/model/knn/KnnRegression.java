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

package rapaio.ml.model.knn;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.pow;

import static rapaio.math.MathTools.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.TreeSet;

import rapaio.core.distributions.Normal;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarType;
import rapaio.math.linear.DVector;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.distance.Distance;
import rapaio.ml.common.distance.EuclideanDistance;
import rapaio.ml.common.param.ValueParam;
import rapaio.ml.model.RegressionModel;
import rapaio.ml.model.RegressionResult;
import rapaio.ml.model.RunInfo;

/**
 * Implements K Nearest Neighbour regression and Weighted K Nearest neighbours.
 * <p>
 * Implementation of Weighted KNN follows:
 * <a href="https://epub.ub.uni-muenchen.de/1769/1/paper_399.pdf">Weighted k-Nearest-Neighbor Techniques
 * and Ordinal Classification, Klaus Hechenbichler, Klaus Schliep, 13th October 2004</a>
 * <p>
 * Optimal weights are implemented following
 * <a href="https://arxiv.org/pdf/1101.5783.pdf">OPTIMAL WEIGHTED NEAREST NEIGHBOUR CLASSIFIERS,  Richard J. Samworth, 2012</a>
 */
public class KnnRegression extends RegressionModel<KnnRegression, RegressionResult, RunInfo<KnnRegression>> {

    public static KnnRegression newModel() {
        return new KnnRegression();
    }

    /**
     * Number of neighbours to consider
     */
    public final ValueParam<Integer, KnnRegression> k = new ValueParam<>(this, 1, "k", v -> Objects.nonNull(v) && v > 0);

    /**
     * Distance function used to select closest points
     */
    public final ValueParam<Distance, KnnRegression> distance = new ValueParam<>(this, new EuclideanDistance(), "distance");

    /**
     * Distance function used to as basis for computing weights
     */
    public final ValueParam<Distance, KnnRegression> wdistance = new ValueParam<>(this, new EuclideanDistance(), "wdistance");

    /**
     * Kernel function used to transform distances computed with {@link #wdistance} into similarities.
     */
    public final ValueParam<Kernel, KnnRegression> kernel = new ValueParam<>(this, Kernel.RECTANGULAR, "kernel");

    /**
     * Small positive quantity used to cut normalized weight distances to avoid division by zero
     */
    public final ValueParam<Double, KnnRegression> eps = new ValueParam<>(this, 1e-6, "eps");

    private DVector[] instances;
    private DVector target;

    @Override
    public KnnRegression newInstance() {
        return new KnnRegression().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "KnnRegression";
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .inputs(1, Integer.MAX_VALUE, false, VarType.DOUBLE, VarType.INT, VarType.LONG, VarType.BINARY)
                .targets(1, 1, false, VarType.DOUBLE, VarType.INT, VarType.LONG, VarType.BINARY);
    }

    private DVector buildInstance(Frame df, int row) {
        DVector instance = DVector.zeros(inputNames.length);
        for (int j = 0; j < inputNames.length; j++) {
            instance.set(j, df.getDouble(row, inputNames[j]));
        }
        return instance;
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        if (df.rowCount() < 2) {
            throw new IllegalArgumentException("Not enough data for regression.");
        }
        this.instances = new DVector[df.rowCount()];
        for (int i = 0; i < df.rowCount(); i++) {
            instances[i] = buildInstance(df, i);
        }
        this.target = df.rvar(targetNames[0]).dv();
        return true;
    }

    private int[] computeTop(DVector[] instances, DVector x, int k) {
        TreeSet<Integer> top = new TreeSet<>((o1, o2) -> {
            double d1 = distance.get().compute(instances[o1], x);
            double d2 = distance.get().compute(instances[o2], x);
            return Double.compare(d1, d2);
        });
        for (int i = 0; i < instances.length; i++) {
            top.add(i);
            if (top.size() > k) {
                top.remove(top.last());
            }
        }
        int[] indexes = new int[k];
        int pos = 0;
        for (int i : top) {
            indexes[pos++] = i;
        }
        return indexes;
    }

    private DVector computeWeights(int[] top, int ref, DVector x) {

        var d = distance.get();
        DVector w = DVector.from(top.length, i -> d.compute(x, instances[top[i]]));

        double wref = d.compute(x, instances[ref]);
        // normalize by k+1 distance
        w.apply(v -> v / wref);
        // cut values to avoid division by zero
        w.apply(v -> min(v, 1 - eps.get()));
        w.apply(v -> max(v, eps.get()));
        // transform into similarity
        w = kernel.get().transform(w, k.get());
        return w;
    }

    @Override
    protected RegressionResult corePredict(Frame df, boolean withResiduals, double[] quantiles) {
        RegressionResult result = RegressionResult.build(this, df, withResiduals, quantiles);

        VarDouble prediction = result.firstPrediction();
        for (int i = 0; i < prediction.size(); i++) {
            DVector x = buildInstance(df, i);

            int[] topIndexesEx = computeTop(instances, x, k.get() + 1);
            int[] topIndexes = Arrays.copyOf(topIndexesEx, topIndexesEx.length - 1);
            int ref = topIndexesEx[topIndexesEx.length - 1];
            DVector weights = computeWeights(topIndexes, ref, x);
            prediction.setDouble(i, target.mapNew(topIndexes).mul(weights).sum() / weights.sum());
        }
        result.buildComplete();
        return result;
    }

    // TODO: implement rank W <- (k+1)-t(apply(as.matrix(D),1,rank))
    // TODO: implement optimal W <- rep(optKernel(k, d=d), each=p)
    public enum Kernel {
        INV {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> 1 / v);
            }
        },
        RECTANGULAR {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? 0.5 : 0);
            }
        },
        TRIANGLUAR {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? 1 - v : 0);
            }
        },
        COS {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? PI * cos(v * HALF_PI) / 4 : 0);
            }
        },
        EPANECHNIKOV {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? 0.75 * (1 - v * v) : 0);
            }
        },
        BIWEIGHT {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? 15 * pow(1 - v * v, 2) / 16 : 0);
            }
        },
        TRIWEIGHT {
            @Override
            public DVector transform(DVector d, int k) {
                return d.applyNew(v -> abs(v) <= 1 ? 35 * pow(1 - v * v, 3) / 32 : 0);
            }
        },
        GAUSSIAN {

            private static final Normal normal = Normal.std();

            @Override
            public DVector transform(DVector d, int k) {
                double alpha = 1.0 / (2 * (k + 1));
                double qua = abs(normal.quantile(alpha));
                return d.applyNew(v -> normal.pdf(v * qua));
            }
        };

        public abstract DVector transform(DVector d, int k);

    }
}
