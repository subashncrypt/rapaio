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

package rapaio.experiment.demo.svm;

import static rapaio.graphics.Plotter.*;

import java.util.logging.Level;

import rapaio.core.distributions.Normal;
import rapaio.core.distributions.Uniform;
import rapaio.core.tools.Grid2D;
import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.VarDouble;
import rapaio.graphics.opt.Palette;
import rapaio.graphics.plot.GridLayer;
import rapaio.math.linear.DMatrix;
import rapaio.math.linear.DVector;
import rapaio.ml.common.kernel.RBFKernel;
import rapaio.ml.model.svm.OneClassSvm;
import rapaio.sys.WS;
import rapaio.sys.With;

public class OneClassSvmDemo {

    public static void main(String[] args) {
        new OneClassSvmDemo().run();
    }

    private static final int N = 20000;
    private static final int M = N / 10;

    void run() {

        WS.initLog(Level.SEVERE);

        Normal normal1 = Normal.of(0, 2);
        Normal normal2 = Normal.of(1, 3);
        VarDouble x1 = VarDouble.from(N, row -> normal1.sampleNext()).name("x1");
        VarDouble x2 = VarDouble.from(N, row -> x1.getDouble(row) * 2 + normal2.sampleNext()).name("x2");

        // add some random uniform
        Uniform uniform = new Uniform(-10, 10);
        for (int i = 0; i < M; i++) {
            x1.addDouble(uniform.sampleNext());
            x2.addDouble(uniform.sampleNext());
        }
        Frame df = SolidFrame.byVars(x1, x2);
        DMatrix xs = DMatrix.copy(df);

        VarDouble gammas = VarDouble.seq(0.001, 0.075, 0.01);

        int len = (int) (Math.floor(Math.sqrt(gammas.size())) + 1);
        GridLayer gl = GridLayer.of(len, len);
        for (double gamma : gammas) {
            OneClassSvm ocs = OneClassSvm
                    .newModel()
                    .kernel.set(RBFKernel.fromGamma(gamma))
                    .nu.set(0.1)
                    .seed.set(42L)
                    .fit(df);

            Grid2D gd = Grid2D.fromFunction((v1, v2) -> ocs.predict(DVector.wrap(v1, v2))
                    .scores().getDouble(0), x1.dv().min(), x1.dv().max(), x2.dv().min(), x2.dv().max(), 64);

            double min = gd.minValue() * 1.1;
            double max = gd.maxValue() * 1.1;
            gl.add(isoBands(gd, VarDouble.seq(min, max, 1).elements(),
                            With.palette(Palette.hue(0, 240, min, max)))
//                    .points(x1, x2)
            );
        }

        WS.draw(gl);
    }
}
