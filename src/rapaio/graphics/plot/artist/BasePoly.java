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

package rapaio.graphics.plot.artist;

import java.awt.Graphics2D;
import java.io.Serial;

import rapaio.data.Var;
import rapaio.graphics.opt.GOption;
import rapaio.graphics.plot.Artist;
import rapaio.graphics.plot.Axis;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/27/20.
 */
public abstract class BasePoly extends Artist {

    @Serial
    private static final long serialVersionUID = 5977928265266862160L;
    protected final Var x;
    protected final Var y;

    public BasePoly(Var x, Var y, GOption<?>... options) {
        this.x = x;
        this.y = y;
        this.options.bind(options);
    }

    @Override
    public Axis.Type xAxisType() {
        return Axis.Type.newNumeric();
    }

    @Override
    public Axis.Type yAxisType() {
        return Axis.Type.newNumeric();
    }

    @Override
    public void updateDataRange(Graphics2D g2d) {
        int len = Math.min(x.size(), y.size());
        for (int i = 0; i < len; i++) {
            union(x.getDouble(i), y.getDouble(i));
        }
    }

}
