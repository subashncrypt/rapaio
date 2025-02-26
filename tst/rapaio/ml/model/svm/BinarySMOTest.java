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

package rapaio.ml.model.svm;


import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import rapaio.core.SamplingTools;
import rapaio.data.Frame;
import rapaio.data.VarRange;
import rapaio.data.preprocessing.StandardScaler;
import rapaio.datasets.Datasets;
import rapaio.ml.common.kernel.CauchyKernel;
import rapaio.ml.common.kernel.ExponentialKernel;
import rapaio.ml.common.kernel.GeneralizedMinKernel;
import rapaio.ml.common.kernel.GeneralizedStudentTKernel;
import rapaio.ml.common.kernel.InverseMultiQuadricKernel;
import rapaio.ml.common.kernel.Kernel;
import rapaio.ml.common.kernel.LogKernel;
import rapaio.ml.common.kernel.MinKernel;
import rapaio.ml.common.kernel.MultiQuadricKernel;
import rapaio.ml.common.kernel.PolyKernel;
import rapaio.ml.common.kernel.RBFKernel;
import rapaio.ml.common.kernel.RationalQuadraticKernel;
import rapaio.ml.common.kernel.SigmoidKernel;
import rapaio.ml.common.kernel.SphericalKernel;
import rapaio.ml.common.kernel.WaveKernel;
import rapaio.ml.common.kernel.WaveletKernel;
import rapaio.ml.eval.ClassifierEvaluation;
import rapaio.ml.eval.metric.Accuracy;
import rapaio.ml.eval.metric.Confusion;
import rapaio.ml.eval.split.StratifiedKFold;

/**
 * Test for binary smo
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 1/20/16.
 */
public class BinarySMOTest {

    @Test
    void testLinear() throws IOException {
        Frame df = Datasets.loadSonar();
        df.copy().fapply(StandardScaler.on(VarRange.all()));

        BinarySMO smo1 = BinarySMO.newModel()
                .kernel.set(new PolyKernel(1))
                .c.set(.1)
                .seed.set(1L);

        var result = ClassifierEvaluation
                .eval(df.fapply(StandardScaler.on(VarRange.all())), "Class", smo1, Accuracy.newMetric(true))
                .splitStrategy.set(new StratifiedKFold(10, "Class"))
                .seed.set(1L)
                .run();
        assertEquals(0.8953094777562862, result.getMeanTrainScore(Accuracy.newMetric(true).getName()), 1e-7);
    }

    @Test
    void testMultipleKernels() throws IOException {

        Frame df = Datasets.loadSonar();

        List<Kernel> kernels = new ArrayList<>();
        kernels.add(new PolyKernel(1));
        kernels.add(new PolyKernel(2));
        kernels.add(new PolyKernel(3));
        kernels.add(new RBFKernel(0.1));
        kernels.add(new LogKernel(1));
//        kernels.add(new SplineKernel());
        kernels.add(new MinKernel());
//        kernels.add(new ChiSquareKernel());
        kernels.add(new CauchyKernel(10));
        kernels.add(new WaveKernel(13));
        kernels.add(new WaveletKernel(0.01));
        kernels.add(new ExponentialKernel());
        kernels.add(new GeneralizedMinKernel(1, 1));
        kernels.add(new GeneralizedStudentTKernel(1));
        kernels.add(new InverseMultiQuadricKernel(1));
        kernels.add(new SphericalKernel(1000));
        kernels.add(new SigmoidKernel(1, 1));
        kernels.add(new MultiQuadricKernel(1));
//        kernels.add(new PowerKernel(2));
        kernels.add(new RationalQuadraticKernel(1));

        for (Kernel k : kernels) {
            BinarySMO smo = BinarySMO.newModel()
                    .kernel.set(k)
                    .maxRuns.set(10)
                    .seed.set(2L);
            df = df.fapply(StandardScaler.on(VarRange.all()));
            double s = ClassifierEvaluation.cv(df, "Class", smo, 3, Accuracy.newMetric())
                    .seed.set(42L)
                    .run()
                    .getMeanTestScore(Accuracy.newMetric().getName());
            assertTrue(s > 0.4, String.format("kernel: %s, accuracy: %.3f", k.name(), s));
        }
    }

    @Test
    void printerTest() {
        var iris = Datasets.loadIrisDataset().stream()
                .filter(s -> s.getLabel("class").equals("versicolor") || s.getLabel("class").equals("setosa"))
                .toMappedFrame()
                .copy();
        BinarySMO smo = BinarySMO.newModel()
                .eps.set(0.3)
                .c.set(17.0)
                .firstLabel.set("versicolor")
                .secondLabel.set("setosa")
                .kernel.set(new LogKernel(1))
                .maxRuns.set(200)
                .seed.set(1L);

        var tts = SamplingTools.trainTestSplit(new Random(1), iris, 0.7);

        assertEquals("BinarySMO{c=17,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=200," +
                        "secondLabel=setosa,seed=1}, fitted=false",
                smo.toString());
        assertEquals("BinarySMO{c=17,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=200," +
                        "secondLabel=setosa,seed=1}",
                smo.fullName());
        assertEquals("""
                BinarySMO model
                ===============
                BinarySMO{c=17,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=200,secondLabel=setosa,seed=1}
                fitted: false.
                """, smo.toSummary());
        assertEquals(smo.toSummary(), smo.toContent());
        assertEquals(smo.toSummary(), smo.toFullContent());

        smo.c.set(100.0).maxRuns.set(10);
        smo.fit(tts.trainDf(), "class");

        assertEquals("BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=10,secondLabel=setosa,seed=1}, " +
                        "fitted=true, support vectors=6",
                smo.toString());
        assertEquals("BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=10," +
                        "secondLabel=setosa,seed=1}",
                smo.fullName());
        assertEquals("""
                BinarySMO model
                ===============
                BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=Log(degree=1),maxRuns=10,secondLabel=setosa,seed=1}
                fitted: true, support vectors=6
                Decision function:
                   0.2643514 * <[4.5,2.3,1.3,0.3], x>
                 - 0.1108114 * <[5.9,3.2,4.8,1.8], x>
                 - 0.4685095 * <[4.9,2.4,3.3,1], x>
                 - 0.1656843 * <[5.8,2.7,4.1,1], x>
                 + 0.192999 * <[5,3,1.6,0.2], x>
                 + 0.2876547 * <[5.4,3.9,1.7,0.4], x>
                 - 0.0594024""", smo.toSummary());
        assertEquals(smo.toSummary(), smo.toContent());
        assertEquals(smo.toSummary(), smo.toFullContent());

        smo.kernel.set(new PolyKernel(1));
        smo.fit(tts.trainDf(), "class");

        assertEquals(
                "BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=PolyKernel(exp=1,bias=1,slope=1),maxRuns=10,secondLabel=setosa,seed=1}, " +
                        "fitted=true, fitted weights=4", smo.toString());
        assertEquals("BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=PolyKernel(exp=1,bias=1,slope=1),maxRuns=10,secondLabel=setosa,seed=1}",
                smo.fullName());
        assertEquals("""
                BinarySMO model
                ===============
                BinarySMO{c=100,eps=0.3,firstLabel=versicolor,kernel=PolyKernel(exp=1,bias=1,slope=1),maxRuns=10,secondLabel=setosa,seed=1}
                fitted: true, fitted weights=4
                Decision function:
                Linear support vector: use attribute weights folding.
                   0.0393929 * [sepal-length]
                 + 0.4876855 * [sepal-width]
                 - 0.9204967 * [petal-length]
                 - 0.5183925 * [petal-width]
                 + 0.9250336""", smo.toSummary());
    }

    @Test
    void solverTest() throws IOException {
        var sonar = Datasets.loadSonar();
        var smo1 = BinarySMO.newModel().kernel.set(new RBFKernel(0.6)).solver.set("Keerthi1").maxRuns.set(1000);
        var smo2 = BinarySMO.newModel().kernel.set(new RBFKernel(0.6)).solver.set("Keerthi2").maxRuns.set(1000);

        var tts = SamplingTools.trainTestSplit(sonar, 0.5);

        var yHat1 = smo1.fit(tts.trainDf(), "Class").predict(tts.testDf()).firstClasses();
        var yHat2 = smo2.fit(tts.trainDf(), "Class").predict(tts.testDf()).firstClasses();

        var accuracy1 = Confusion.from(tts.testDf().rvar("Class"), yHat1).accuracy();
        var accuracy2 = Confusion.from(tts.testDf().rvar("Class"), yHat2).accuracy();

        // performance should not be very different
        assertEquals(accuracy1, accuracy2, 1e-7);
    }
}
