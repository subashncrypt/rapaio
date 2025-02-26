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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.io.Serial;

import rapaio.data.Var;
import rapaio.graphics.opt.GOption;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/26/20.
 */
public class PolyFill extends BasePoly {

    @Serial
    private static final long serialVersionUID = 8507830080358347197L;

    public PolyFill(PolyPath polyPath, GOption<?>...options) {
        super(polyPath.getPath().v1, polyPath.getPath().v2, options);
    }

    public PolyFill(Var x, Var y, GOption<?>... options) {
        super(x, y, options);
    }

    @Override
    public void paint(Graphics2D g2d) {

        Path2D.Double path = new Path2D.Double();
        int len = Math.min(x.size(), y.size());
        path.moveTo(xScale(x.getDouble(0)), yScale(y.getDouble(0)));
        for (int i = 1; i < len; i++) {
            path.lineTo(xScale(x.getDouble(i)), yScale(y.getDouble(i)));
        }
        path.lineTo(xScale(x.getDouble(0)), yScale(y.getDouble(0)));

        g2d.setStroke(new BasicStroke(options.getLwd()));
        g2d.setColor(options.getFill(0));

        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, options.getAlpha()));
        g2d.fill(path);
        g2d.setComposite(oldComposite);
    }
}
