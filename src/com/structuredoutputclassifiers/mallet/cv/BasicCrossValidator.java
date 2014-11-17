package com.structuredoutputclassifiers.mallet.cv;

import cc.mallet.types.*;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.MalletUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Author: Marcin Dobrowolski
 */
public class BasicCrossValidator extends CrossValidator {

    private int[] splitIndices;
    private Alphabet coreFeatures;

    private InstanceList[] buckets;

    public BasicCrossValidator() {
        super();
        splitIndices = new int[2];
        setTrainingSetSizeProportions(0.5);
    }

    public InstanceListWithCoreFeatures[] nextSplit() {
        InstanceListWithCoreFeatures trainingSet;
        InstanceListWithCoreFeatures testSet;
        Alphabet dataAlphabet = buckets[0].getDataAlphabet();
        trainingSet = new InstanceListWithCoreFeatures(dataAlphabet, buckets[0].getTargetAlphabet(), coreFeatures);
        testSet = new InstanceListWithCoreFeatures(dataAlphabet, buckets[0].getTargetAlphabet(), coreFeatures);
        InstanceListWithCoreFeatures setToGrow;
        for (int i = 0; i < buckets.length; i++) {
            if (i >= splitIndices[0] && i < splitIndices[1]) {
                setToGrow = testSet;
            } else if (splitIndices[0] > splitIndices[1] && (i >= splitIndices[0] || i < splitIndices[1])) {
                setToGrow = testSet;
            } else {
                setToGrow = trainingSet;
            }
            for (Instance instance : buckets[i]) {
                setToGrow.add(instance);
            }
        }
        splitIndices[0] = (splitIndices[0] + 1) % 10;
        splitIndices[1] = (splitIndices[1] + 1) % 10;
        return new InstanceListWithCoreFeatures[]{trainingSet, testSet};
    }

    @Override
    public void setInstanceList(InstanceListWithCoreFeatures instanceList) {
        double[] proportions = new double[10];
        Arrays.fill(proportions, 0.1);
        buckets = instanceList.split(new Random(seed), proportions);
        coreFeatures = instanceList.getCoreFeatures();
    }

    @Override
    public void setTrainingSetSizeProportions(double trainingSetSizeProportion) {
        super.setTrainingSetSizeProportions(trainingSetSizeProportion);
        splitIndices[0] = new Random(seed).nextInt(10);
        int offset = (int) (10 - trainingSetSizeProportion * 10);
        splitIndices[1] = (splitIndices[0] + offset) % 10;
    }
}
