/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package rapaio.supervised.tree;

import org.junit.Test;
import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.Vector;
import rapaio.explore.Summary;
import rapaio.filters.BaseFilters;
import rapaio.io.ArffPersistence;
import rapaio.io.CsvPersistence;
import rapaio.supervised.ModelEvaluation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class RandomForestTest {

    public static Frame loadFrame(String name) throws IOException {
        final String path = "/UCI/" + name + ".arff";
        ArffPersistence arff = new ArffPersistence();
        return arff.read(name, RandomForestTest.class.getResourceAsStream(path));
    }

    public double test(String name) throws IOException {
        Frame df = loadFrame(name);
        String className = df.getCol(df.getColCount() - 1).getName();
        RandomForest rf = new RandomForest(){{
            setMtrees(100);
        }};
        ModelEvaluation cv = new ModelEvaluation();
        return cv.cv(df, className, rf, 10);
    }

        @Test
    public void allCompareTest() throws IOException, URISyntaxException {
        CsvPersistence csv = new CsvPersistence();
        csv.setHasHeader(true);
        Frame tests = csv.read("test", Paths.get(getClass().getResource("tests.csv").toURI()));
        for (int i = 0; i < tests.getRowCount(); i++) {
            if (tests.getLabel(i, 0).startsWith("#")) {
                continue;
            }
            System.out.println("test for " + tests.getLabel(i, 0));
            tests.setValue(i, 3, test(tests.getLabel(i, 0)));
        }
        Summary.head(tests.getRowCount(), tests);
    }
}
