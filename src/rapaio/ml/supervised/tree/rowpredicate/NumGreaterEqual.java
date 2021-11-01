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

package rapaio.ml.supervised.tree.rowpredicate;

import java.io.Serial;

import rapaio.data.Frame;
import rapaio.ml.supervised.tree.RowPredicate;
import rapaio.printer.Format;

public record NumGreaterEqual(String testName, double testValue) implements RowPredicate {

    @Serial
    private static final long serialVersionUID = 8904590203760623732L;

    @Override
    public boolean test(int row, Frame df) {
        if (df.isMissing(row, testName)) {
            return false;
        }
        return df.getDouble(row, testName) >= testValue;
    }

    @Override
    public String toString() {
        return testName + ">=" + Format.floatFlex(testValue);
    }
}
