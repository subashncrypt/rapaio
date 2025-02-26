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

package rapaio.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import rapaio.datasets.Datasets;
import rapaio.io.JavaIO;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 9/17/15.
 */
public class SerializationTest {

    @Test
    void testFrames() throws IOException, ClassNotFoundException {
        testFrame(Datasets.loadIrisDataset());
        testFrame(Datasets.loadCarMpgDataset());
        testFrame(Datasets.loadRandom(new Random()));
    }

    void testFrame(Frame df) throws IOException, ClassNotFoundException {
        File tmp = File.createTempFile("test-", "ser");
        JavaIO.storeToFile(df, tmp);
        Frame restore = (Frame) JavaIO.restoreFromFile(tmp);
        assertTrue(df.deepEquals(restore));
    }
}
