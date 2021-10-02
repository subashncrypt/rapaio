/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2021 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.core.distributions.empirical;

import java.io.Serial;

import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

/**
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class KFuncUniform implements KFunc {

    @Serial
    private static final long serialVersionUID = 3237652502168949601L;

    @Override
    public double pdf(double x, double x0, double bandwidth) {
        double value = Math.abs(x - x0) / bandwidth;
        if (value <= 1) return 0.5;
        return 0;
    }

    @Override
    public double minValue(double x, double bandwidth) {
        return x - bandwidth;
    }

    @Override
    public double maxValue(double x, double bandwidth) {
        return x + bandwidth;
    }

    @Override
    public String toString() {
        return "KFuncUniform";
    }

    @Override
    public String toContent(Printer printer, POption<?>... options) {
        return toString();
    }
}
