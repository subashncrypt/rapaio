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

package rapaio.ml.tree;

import rapaio.data.Frame;
import rapaio.data.Frames;
import rapaio.data.Nominal;
import rapaio.data.Vector;
import rapaio.data.stream.FSpot;
import rapaio.ml.AbstractClassifier;
import rapaio.ml.Classifier;
import rapaio.ml.tools.DensityVector;
import rapaio.ml.tools.TreeCTest;

import java.util.Iterator;
import java.util.List;

/**
 * User: Aurelian Tutuianu <paderati@yahoo.com>
 */
public class DecisionStumpClassifier extends AbstractClassifier<DecisionStumpClassifier> {

    private int minCount = 1;
    private TreeCTest.Method method = TreeCTest.Method.INFO_GAIN;

    private String[] dict;

    private TreeCTest test = new TreeCTest(method, minCount);

    private String leftLabel;
    private String rightLabel;
    private String defaultLabel;
    private int leftIndex;
    private int rightIndex;
    private int defaultIndex;

    private Nominal pred;
    private Frame dist;

    @Override
    public Classifier newInstance() {
        return new DecisionStumpClassifier().withMethod(method).withMinCount(minCount);
    }

    public DecisionStumpClassifier withMinCount(int minCount) {
        this.minCount = minCount;
        test = new TreeCTest(method, minCount);
        return this;
    }

    public DecisionStumpClassifier withMethod(TreeCTest.Method method) {
        this.method = method;
        test = new TreeCTest(method, minCount);
        return this;
    }

    @Override
    public void learn(Frame df, List<Double> weights, String targetColName) {

        dict = df.col(targetColName).getDictionary();

        // find best split test and eventually split point

        for (String colName : df.colNames()) {
            if (targetColName.equals(colName)) continue;
            if (df.col(colName).type().isNumeric()) {
                test.binaryNumericTest(df, colName, targetColName, weights);
            } else {
                for (String testLabel : df.col(colName).getDictionary()) {
                    if (testLabel.equals("?")) continue;
                    test.binaryNominalTest(df, colName, targetColName, weights, testLabel);
                }
            }
        }

        if (test.testName() != null) {

            // we have something, we evaluate both branches

            String testName = test.testName();
            DensityVector left = new DensityVector(dict);
            DensityVector right = new DensityVector(dict);
            DensityVector missing = new DensityVector(dict);

            Vector testVector = df.col(testName);

            // update density vectors in order to predict

            for (int i = 0; i < df.rowCount(); i++) {
                if (testVector.isMissing(i)) {
                    missing.update(df.col(targetColName).getIndex(i), weights.get(i));
                    continue;
                }
                boolean onLeft = true;
                if (testVector.type().isNominal() && !test.splitLabel().equals(testVector.getLabel(i))) {
                    onLeft = false;
                }
                if (testVector.type().isNumeric() && test.splitValue() < testVector.getValue(i)) {
                    onLeft = false;
                }
                (onLeft ? left : right).update(df.col(targetColName).getIndex(i), weights.get(i));
            }

            // now predict

            leftIndex = left.findBestIndex(false);
            leftLabel = dict[leftIndex];
            rightIndex = right.findBestIndex(false);
            rightLabel = dict[rightIndex];
            defaultIndex = missing.findBestIndex(false);
            defaultLabel = dict[defaultIndex];
        } else {

            // we found nothing, predict with majority

            DensityVector missing = new DensityVector(dict);
            for (int i = 0; i < df.rowCount(); i++) {
                missing.update(df.col(targetColName).getIndex(i), weights.get(i));
            }
            defaultIndex = missing.findBestIndex(false);
            defaultLabel = dict[defaultIndex];
        }
    }

    @Override
    public void predict(Frame df) {
        pred = new Nominal(df.rowCount(), dict);
        dist = Frames.newMatrix(df.rowCount(), dict);

        Iterator<FSpot> it = df.stream().iterator();
        while (it.hasNext()) {
            FSpot f = it.next();

            if (test.testName() == null || f.isMissing(test.testName())) {
                dist.setValue(f.row(), defaultIndex, 1.0);
                pred.setLabel(f.row(), defaultLabel);
                continue;
            }
            if (df.col(test.testName()).type().isNumeric()) {
                if (f.getValue(test.testName()) <= test.splitValue()) {
                    dist.setValue(f.row(), leftIndex, 1.0);
                    pred.setLabel(f.row(), leftLabel);
                } else {
                    dist.setValue(f.row(), rightIndex, 1.0);
                    pred.setLabel(f.row(), rightLabel);
                }
            } else {
                if (test.splitLabel().equals(f.getLabel(test.testName()))) {
                    dist.setValue(f.row(), leftIndex, 1.0);
                    pred.setLabel(f.row(), leftLabel);
                } else {
                    dist.setValue(f.row(), rightIndex, 1.0);
                    pred.setLabel(f.row(), rightLabel);
                }
            }
        }
    }

    @Override
    public Nominal prediction() {
        return pred;
    }

    @Override
    public Frame distribution() {
        return dist;
    }

    @Override
    public void summary() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
