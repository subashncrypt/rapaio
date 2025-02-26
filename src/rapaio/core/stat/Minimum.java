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

package rapaio.core.stat;

import static rapaio.printer.Format.floatFlex;

import rapaio.data.Var;
import rapaio.printer.Printable;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

/**
 * Finds the minimum value from a {@link rapaio.data.Var} of values.
 * <p>
 * Ignores missing elements.
 * <p>
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 * Date: 9/7/13
 * Time: 12:36 PM
 */
public class Minimum implements Printable {

    public static Minimum of(Var var) {
        return new Minimum(var);
    }

    private final String varName;
    private double value;
    private int completeCount;
    private int missingCount;

    private Minimum(Var var) {
        this.varName = var.name();
        this.value = Double.NaN;
        for (int i = 0; i < var.size(); i++) {
            if (var.isMissing(i)) {
                missingCount++;
            } else {
                completeCount++;
                if (Double.isNaN(value) || value > var.getDouble(i)) {
                    value = var.getDouble(i);
                }
            }
        }
    }

    public double value() {
        return value;
    }

    @Override
    public String toString() {
        return "minimum[" + varName + "] = " + floatFlex(value);
    }

    @Override
    public String toContent(Printer printer, POption<?>... options) {
        return "> minimum[" + varName + "]\n" +
                "total rows: " + (completeCount + missingCount) + " (complete: " + completeCount + ", missing: " + missingCount + ")\n" +
                "minimum: " + floatFlex(value) + "\n";
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
