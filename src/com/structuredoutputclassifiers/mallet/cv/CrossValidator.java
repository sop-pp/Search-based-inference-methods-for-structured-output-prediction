package com.structuredoutputclassifiers.mallet.cv;

import cc.mallet.types.InstanceList;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;

/**
 * Author: Marcin Dobrowolski
 */
public abstract class CrossValidator {

    protected int seed = 1;
    protected double trainingSetSizeProportion = 0.9;

    public abstract InstanceListWithCoreFeatures[] nextSplit();

    public abstract void setInstanceList(InstanceListWithCoreFeatures instanceList);

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void setTrainingSetSizeProportions(double trainingSetSizeProportion) {
        this.trainingSetSizeProportion = Math.floor(trainingSetSizeProportion * 10) / 10;
    }
}
