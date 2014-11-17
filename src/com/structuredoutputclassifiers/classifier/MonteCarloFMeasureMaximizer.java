package com.structuredoutputclassifiers.classifier;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.Sequence;
import put.mlc.classifiers.f.FMeasure;

import java.util.Comparator;
import java.util.Random;

/**
 * Author: Marcin Dobrowolski
 */
public class MonteCarloFMeasureMaximizer extends LogisticRegressionClassifier {

    private int sampleSize = 10000;

    public MonteCarloFMeasureMaximizer(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    @Override
    public Sequence classify(Instance instance) {
        FMeasure fMeasure = new FMeasure();
        Random r = new Random(seed);
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        int nonEntityLabelIndex = labelAlphabet.lookupIndex("O", false);
        Instance[] instances = splitSequence(instance, instance.getDataAlphabet(),
                labelAlphabet);
        fMeasure.initialize(instances.length);
        byte[] sample = new byte[instances.length];
        double[][] scores = new double[instances.length][labelAlphabet.size()];
        for (int i = 0; i < instances.length; i++) {
            classifier.getClassificationScores(instances[i], scores[i]);
        }
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < instances.length; j++) {
                double nonEntityScore = scores[j][nonEntityLabelIndex];
                sample[j] = (byte) (r.nextDouble() >= nonEntityScore ? 1 : 0);
            }
            fMeasure.add(sample);
        }
        fMeasure.computeFMeasureMaximizer();
        mulan.classifier.MultiLabelOutput multiLabelOutput = fMeasure.computePrediction();
        boolean[] areEntities = multiLabelOutput.getBipartition();
        Sequence sequence = new Sequence();
        for (boolean isEntity : areEntities) {
            sequence.add(isEntity ? "E" : "O");
        }
        sequence.setComparator(new BinaryEntityComparator());
        return sequence;
    }

    @Override
    public void trainClassifier(InstanceListWithCoreFeatures trainingInstances) {
        MaxEntTrainer trainer = new MaxEntTrainer();
        trainer.setGaussianPriorVariance(1);
        trainer.train(splitSequenceIntoSingleInstanceList(trainingInstances));
        classifier = trainer.getClassifier();
    }

    public void trainClassifier(InstanceListWithCoreFeatures trainingInstances, double param) {
        MaxEntTrainer trainer = new MaxEntTrainer();
        trainer.setGaussianPriorVariance(param);
        trainer.train(splitSequenceIntoSingleInstanceList(trainingInstances));
        classifier = trainer.getClassifier();
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public class BinaryEntityComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return (o1.equals("O") && o2.equals("O")) || (!o1.equals("O") && !o2.equals("O")) ? 0 : 1;
        }
    }
}
