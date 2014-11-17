package com.structuredoutputclassifiers.util;

import cc.mallet.types.Instance;
import cc.mallet.types.LabelSequence;

/**
 * Author: Marcin Dobrowolski
 */
public class MalletSequenceClassificationResult {

    private long classificationTimeInMiliseconds;

    private Instance instance;

    private LabelSequence predictedSequence;

    public long getClassificationTimeInMiliseconds() {
        return classificationTimeInMiliseconds;
    }

    public void setClassificationTimeInMiliseconds(long classificationTimeInMiliseconds) {
        this.classificationTimeInMiliseconds = classificationTimeInMiliseconds;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public LabelSequence getPredictedSequence() {
        return predictedSequence;
    }

    public void setPredictedSequence(LabelSequence predictedSequence) {
        this.predictedSequence = predictedSequence;
    }
}
