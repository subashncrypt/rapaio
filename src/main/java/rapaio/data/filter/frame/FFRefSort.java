/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
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
 *
 */

package rapaio.data.filter.frame;

import rapaio.data.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 12/5/14.
 */
public class FFRefSort extends FFAbstract {

    private static final long serialVersionUID = 3579078253849199109L;

    private final Comparator<Integer> aggregateComparator;

    @SafeVarargs
    public FFRefSort(Comparator<Integer>... comparators) {
        super(VRange.of("all"));
        this.aggregateComparator = RowComparators.aggregate(comparators);
    }

    @Override
    public void fit(Frame df) {
    }

    @Override
    public Frame apply(Frame df) {
        List<Integer> rows = new ArrayList<>(df.rowCount());
        for (int i = 0; i < df.rowCount(); i++) {
            rows.add(i);
        }
        Collections.sort(rows, aggregateComparator);
        return MappedFrame.newByRow(df, Mapping.wrap(rows));
    }
}
