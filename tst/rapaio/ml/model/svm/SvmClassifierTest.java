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
import java.util.Random;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rapaio.data.Frame;
import rapaio.data.VarRange;
import rapaio.data.VarType;
import rapaio.datasets.Datasets;
import rapaio.experiment.ml.svm.libsvm.svm;
import rapaio.experiment.ml.svm.libsvm.svm_model;
import rapaio.experiment.ml.svm.libsvm.svm_predict;
import rapaio.experiment.ml.svm.libsvm.svm_train;
import rapaio.math.linear.DMatrix;
import rapaio.math.linear.DVector;
import rapaio.ml.common.kernel.RBFKernel;
import rapaio.ml.model.ClassifierResult;
import rapaio.sys.WS;

public class SvmClassifierTest {

    private static final double TOL = 1e-16;


    private Frame iris;
    private DMatrix xs;
    private DVector ys;

    @BeforeEach
    void beforeEach() {
        WS.initLog(Level.SEVERE);
        iris = Datasets.loadIrisDataset();

        xs = DMatrix.copy(iris.mapVars(VarRange.onlyTypes(VarType.DOUBLE)));
        ys = DVector.from(xs.rows(), i -> iris.rvar(4).getInt(i) - 1);
    }

    @Test
    void testIrisProbC() throws IOException {
        svm_train t = new svm_train();
        String[] argv = new String[] {"-s", "0",
                "-t", "2",
                "-g", "0.7",
                "-c", "10",
                "-b", "1"};
        svm.rand = new Random(42);
        svm_model model = t.run(xs, ys, argv);
        svm_predict.Prediction pred = svm_predict.predict(model, xs, 1);

        SvmClassifier c = new SvmClassifier()
                .type.set(SvmClassifier.Penalty.C)
                .c.set(10.0)
                .probability.set(true)
                .kernel.set(new RBFKernel(0.7))
                .seed.set(42L);
        ClassifierResult cpred = c.fit(iris, "class").predict(iris);
        DMatrix cdensity = DMatrix.copy(cpred.firstDensity()).removeCols(0);

        assertTrue(pred.density().deepEquals(cdensity, TOL));
        for (int i = 0; i < pred.classes().length; i++) {
            int cls = (int) pred.classes()[i];
            assertEquals(cls + 1, cpred.firstClasses().getInt(i));
        }
    }

    @Test
    void testIrisClassC() throws IOException {
        svm_train t = new svm_train();
        String[] argv = new String[] {"-s", "0",
                "-t", "2",
                "-g", "0.7",
                "-c", "10",
                "-b", "0"};
        svm.rand = new Random(42);
        svm_model model = t.run(xs, ys, argv);
        svm_predict.Prediction pred = svm_predict.predict(model, xs, 1);

        SvmClassifier c = new SvmClassifier()
                .type.set(SvmClassifier.Penalty.C)
                .c.set(10.0)
                .probability.set(false)
                .kernel.set(new RBFKernel(0.7))
                .seed.set(42L);

        ClassifierResult cpred = c.fit(iris, "class").predict(iris, true, true);
        for (int i = 0; i < pred.classes().length; i++) {
            int cls = (int) pred.classes()[i];
            assertEquals(cls + 1, cpred.firstClasses().getInt(i));
        }
    }

    @Test
    void testIrisProbNU() throws IOException {
        svm_train t = new svm_train();
        String[] argv = new String[] {"-s", "1",
                "-t", "2",
                "-g", "0.7",
                "-n", "0.1",
                "-b", "1"};
        svm_model model = t.run(xs, ys, argv);
        svm.rand = new Random(42);
        svm_predict.Prediction pred = svm_predict.predict(model, xs, 1);

        SvmClassifier c = new SvmClassifier()
                .type.set(SvmClassifier.Penalty.NU)
                .nu.set(.1)
                .probability.set(true)
                .kernel.set(new RBFKernel(0.7))
                .seed.set(42L);
        ClassifierResult cpred = c.fit(iris, "class").predict(iris);
        DMatrix cdensity = DMatrix.copy(cpred.firstDensity()).removeCols(0);

        assertTrue(pred.density().deepEquals(cdensity, TOL));
        for (int i = 0; i < pred.classes().length; i++) {
            int cls = (int) pred.classes()[i];
            assertEquals(cls + 1, cpred.firstClasses().getInt(i));
        }
    }

    @Test
    void testIrisClassNu() throws IOException {
        svm_train t = new svm_train();
        String[] argv = new String[] {"-s", "1",
                "-t", "2",
                "-g", "0.7",
                "-n", "0.1",
                "-b", "0"};
        svm.rand = new Random(42);
        svm_model model = t.run(xs, ys, argv);
        svm_predict.Prediction pred = svm_predict.predict(model, xs, 1);

        SvmClassifier c = new SvmClassifier()
                .type.set(SvmClassifier.Penalty.NU)
                .nu.set(.1)
                .probability.set(false)
                .kernel.set(new RBFKernel(0.7))
                .seed.set(42L);

        ClassifierResult cpred = c.fit(iris, "class").predict(iris, true, true);
        for (int i = 0; i < pred.classes().length; i++) {
            int cls = (int) pred.classes()[i];
            assertEquals(cls + 1, cpred.firstClasses().getInt(i));
        }
    }
}
