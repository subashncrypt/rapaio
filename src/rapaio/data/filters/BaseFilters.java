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

package rapaio.data.filters;

import rapaio.core.RandomSource;
import rapaio.data.*;

import java.io.Serializable;
import java.util.*;

/**
 * Provides filters for frames.
 * <p>
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
@Deprecated
public final class BaseFilters implements Serializable {

    private BaseFilters() {
    }


    /**
     * Frame filters
     */
    //=================================================================================


    /**
     * Retain only numeric columns from a frame.
     */
    public static Frame retainNumeric(Frame df) {
        List<Var> vars = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < df.varCount(); i++) {
            if (df.var(i).type().isNumeric()) {
                vars.add(df.var(i));
                names.add(df.varNames()[i]);
            }
        }
        return SolidFrame.newWrapOf(df.rowCount(), vars);
    }

    /**
     * Retain only nominal columns from a frame.
     */
    public static Frame retainNominal(Frame df) {
        List<String> varNames = new ArrayList<>();
        for (int i = 0; i < df.varCount(); i++) {
            if (df.var(i).type().isNominal()) {
                varNames.add(df.varNames()[i]);
            }
        }
        return df.mapVars(varNames);
    }

    /**
     * Consolidates all the common nominal columns from all frames given as parameter.
     *
     * @param source list of source frames
     * @return list of frames with common nominal columns consolidated
     */
    public static List<Frame> consolidate(List<Frame> source) {

        // learn reunion of labels for all columns
        HashMap<String, HashSet<String>> dicts = new HashMap<>();
        for (int i = 0; i < source.size(); i++) {
            for (Frame frame : source) {
                for (String colName : frame.varNames()) {
                    if (!frame.var(colName).type().isNominal()) {
                        continue;
                    }
                    if (!dicts.containsKey(colName)) {
                        dicts.put(colName, new HashSet<>());
                    }
                    dicts.get(colName).addAll(Arrays.asList(frame.var(colName).dictionary()));
                }
            }
        }

        // rebuild each frame according with the new consolidated data
        List<Frame> dest = new ArrayList<>();
        for (Frame frame : source) {
            Var[] vars = new Var[frame.varCount()];
            for (int i = 0; i < frame.varCount(); i++) {
                Var v = frame.var(i);
                String colName = frame.varNames()[i];
                if (!v.type().isNominal()) {
                    vars[i] = v;
                } else {
                    vars[i] = Nominal.newEmpty(v.rowCount(), dicts.get(colName)).withName(colName);
                    for (int k = 0; k < vars[i].rowCount(); k++) {
                        vars[i].setLabel(k, v.label(k));
                    }
                }
            }
            dest.add(SolidFrame.newWrapOf(frame.rowCount(), vars));
        }

        return dest;
    }

    /**
     * Shuffle the order of getRowCount from specified frame.
     *
     * @param df source frame
     * @return shuffled frame
     */
    public static Frame shuffle(Frame df) {
        List<Integer> mapping = new ArrayList<>();
        for (int i = 0; i < df.rowCount(); i++) {
            mapping.add(i);
        }
        for (int i = mapping.size(); i > 1; i--) {
            mapping.set(i - 1, mapping.set(RandomSource.nextInt(i), mapping.get(i - 1)));
        }
        return MappedFrame.newByRow(df, Mapping.newWrapOf(mapping));
    }

    public static List<Frame> combine(List<Frame> frames, String... combined) {
        Set<String> dict = new HashSet<>();
        dict.add("");
        for (Frame frame1 : frames) {
            if (frame1 instanceof MappedFrame) {
                throw new IllegalArgumentException("Not allowed mapped frames");
            }
        }

        for (String aCombined : combined) {
            String[] vdict = frames.get(0).var(aCombined).dictionary();
            Set<String> newdict = new HashSet<>();
            for (String term : dict) {
                for (String aVdict : vdict) {
                    newdict.add(term + "." + aVdict);
                }
            }
            dict = newdict;
        }

        List<Frame> result = new ArrayList<>();
        for (Frame frame : frames) {
            List<Var> vars = new ArrayList<>();
            for (int j = 0; j < frame.varCount(); j++) {
                vars.add(frame.var(j));
            }
            Var col = Nominal.newEmpty(frame.rowCount(), dict);
            for (int j = 0; j < frame.rowCount(); j++) {
                StringBuilder sb = new StringBuilder();
                for (String c : combined) {
                    sb.append(".").append(frame.label(j, frame.varIndex(c)));
                }
                col.setLabel(j, sb.toString());
            }
            vars.add(col);
            result.add(SolidFrame.newWrapOf(frame.rowCount(), vars));
        }
        return result;

    }

}
