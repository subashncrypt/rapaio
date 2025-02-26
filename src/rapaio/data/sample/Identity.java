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

package rapaio.data.sample;

import java.io.Serial;
import java.util.Random;

import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;

/**
 * Identity sampling means the sample is identical with the original set of data
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 1/25/16.
 */
public final class Identity implements RowSampler {

    @Serial
    private static final long serialVersionUID = -1133893495082466752L;

    @Override
    public Sample nextSample(Random random, Frame df, Var weights) {
        return new Sample(df, weights, Mapping.range(0, df.rowCount()), df.rowCount());
    }

    @Override
    public String name() {
        return "Identity";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Identity;
    }
}
