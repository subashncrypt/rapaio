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

package rapaio.data.preprocessing;

import rapaio.data.Frame;
import rapaio.data.VarRange;

/**
 * Frame filter which creates a copy of the input frame. The copied frame is a solid frame.
 */
public class FrameCopy extends AbstractTransform {

    public static FrameCopy transform() {
        return new FrameCopy();
    }

    private FrameCopy() {
        super(VarRange.all());
    }

    @Override
    public FrameCopy newInstance() {
        return new FrameCopy();
    }

    @Override
    protected void coreFit(Frame df) {
    }

    @Override
    public Frame coreApply(Frame df) {
        return df.mapVars(varNames).copy();
    }
}
