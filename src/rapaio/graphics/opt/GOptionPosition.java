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

package rapaio.graphics.opt;

import java.awt.geom.Rectangle2D;
import java.io.Serial;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 4/16/21.
 */
public record GOptionPosition(Rectangle2D rectangle) implements GOption<Rectangle2D> {

    @Serial
    private static final long serialVersionUID = 5320193537092151537L;

    @Override
    public void bind(GOptions opts) {
        opts.setPosition(this);
    }

    @Override
    public Rectangle2D apply(GOptions opts) {
        return rectangle;
    }
}
