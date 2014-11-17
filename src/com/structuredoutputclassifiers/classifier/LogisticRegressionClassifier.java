package com.structuredoutputclassifiers.classifier;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.*;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Marcin Dobrowolski
 */
public class LogisticRegressionClassifier extends RecurrentClassifier {

    protected MaxEnt classifier;
    protected Alphabet coreFeatures;
    private Double gaussian = 1.0;

    public LogisticRegressionClassifier() {
        super();
        params = new LinkedList<>();
        HashMap<String, Object> paramSet = new HashMap<>();
        paramSet.put("gaussian", 1.0);
        params.add(paramSet);
        paramSet = new HashMap<>();
        paramSet.put("gaussian",0.1);
        params.add(paramSet);
    }

    @Override
    public Sequence classify(Instance instance) {
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        String[] states = labelAlphabetToStringArray(labelAlphabet);
        Instance[] instances = splitSequence(instance, null, labelAlphabet);
        StringBuilder sb = new StringBuilder();
        Sequence result = new Sequence();
        LinkedList<String> prev = new LinkedList<>();
        for(int i = 0; i < numBack; i++) {
            prev.add("^");
        }
        for(int i = 0; i < instances.length; i++) {
            MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, prev);
            double[] scores = new double[instance.getTargetAlphabet().size()];
            try {
                classifier.getClassificationScores(instances[i], scores);
            } catch(ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            int maxIndex = MatrixOps.maxIndex(scores);
            String predictedLabel = (String) instance.getTargetAlphabet().lookupObject(maxIndex);
            if(prev.size() > 0) {
                prev.remove(prev.size() - 1);
                prev.add(0, predictedLabel);
            }
            result.add(predictedLabel);
        }
        return result;
    }

    @Override
    public void setCurrentParamSet(HashMap<String, Object> paramSet) {
        this.gaussian = (Double) paramSet.get("gaussian");
    }


    @Override
    public void trainClassifier(InstanceListWithCoreFeatures trainingInstances) {
        MaxEntTrainer trainer = new MaxEntTrainer();
        trainer.setGaussianPriorVariance(gaussian);
        trainer.train(splitSequenceIntoSingleInstanceList(trainingInstances));
        classifier = trainer.getClassifier();
        coreFeatures = trainingInstances.getCoreFeatures();
    }
}
