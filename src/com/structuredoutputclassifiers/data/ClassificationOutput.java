package com.structuredoutputclassifiers.data;

import com.structuredoutputclassifiers.util.Sequence;

import java.util.Comparator;

/**
 * Author: Marcin Dobrowolski
 */

public class ClassificationOutput {
    private Sequence predictions;
    private Sequence trueValues;
    private double classificationTimeInMiliseconds;

    public Sequence getPredictions() {
        return predictions;
    }

    public void setPredictedSequence(Sequence predictions) {
        this.predictions = predictions;
    }

    public Sequence getTrueValues() {
        return trueValues;
    }

    public void setTrueSequence(Sequence trueValues) {
        this.trueValues = trueValues;
    }

    public double precision() {
        double tp = 0;
        double fp = 0;
        Sequence predictions = getPredictions();
        Sequence trueValues = getTrueValues();
        for (int j = 0; j < sequenceSize(); j++) {
            boolean isCorrectEntity = isEntity(trueValues.get(j));
            boolean isPredictedEntity = isEntity(predictions.get(j));
            if (isCorrectEntity && isPredictedEntity) {
                tp++;
            } else if (!isCorrectEntity && isPredictedEntity) {
                fp++;
            }
        }
        return tp + fp == 0 ? 0 : tp / (tp + fp);
    }

    private boolean isEntity(String entity) {
        return !entity.equals("O") && !entity.equals("0");
    }

    public double recall() {
        double tp = 0;
        double fn = 0;
        Sequence predictions = getPredictions();
        Sequence trueValues = getTrueValues();
        for (int j = 0; j < sequenceSize(); j++) {
            boolean isCorrectEntity = isEntity(trueValues.get(j));
            boolean isPredictedEntity = isEntity(predictions.get(j));
            if (isPredictedEntity && isCorrectEntity) {
                tp++;
            } else if (!isPredictedEntity && isCorrectEntity) {
                fn++;
            }
        }
        return tp + fn == 0 ? 0 : tp / (tp + fn);
    }

    public double fMeasure() {
        double precision = precision();
        double recall = recall();
        return precision + recall == 0 ? 0 : (2.0 * precision * recall) / (precision + recall);
    }

    public int correctSinglePredictions() {
        int result = 0;
        Comparator<String> c = predictions.getComparator();
        for (int i = 0; i < sequenceSize(); i++) {
            if (c.compare(trueValues.get(i), predictions.get(i)) == 0) {
                result++;
            }
        }
        return result;
    }

    public int sequenceSize() {
        return predictions.size();
    }

    public boolean isCorrectSequence() {
        return correctSinglePredictions() == sequenceSize();
    }

    public double getClassificationTimeInMiliseconds() {
        return classificationTimeInMiliseconds;
    }

    public void setClassificationTimeInMiliseconds(double classificationTimeInMiliseconds) {
        this.classificationTimeInMiliseconds = classificationTimeInMiliseconds;
    }
}
