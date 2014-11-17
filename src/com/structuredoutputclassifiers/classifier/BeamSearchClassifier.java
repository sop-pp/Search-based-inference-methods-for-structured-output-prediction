package com.structuredoutputclassifiers.classifier;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;

import java.util.*;

/**
 * Author: Marcin Dobrowolski
 */
public class BeamSearchClassifier extends LogisticRegressionClassifier {

    private int beamSize;

    public BeamSearchClassifier(int beamSize) {
        this.beamSize = beamSize;
    }

    private class ScoreObject {
        public double score;
        public Sequence sequence;
    }


    @Override
    public Sequence classify(Instance instance) {
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        String[] states = labelAlphabetToStringArray(labelAlphabet);
        Instance[] instances = splitSequence(instance, null, labelAlphabet);
        double[] scores = new double[states.length];
        List<ScoreObject> beam = new ArrayList<>(instances.length);
        ScoreObject sObject = new ScoreObject();
        sObject.sequence = new Sequence();
        for(int i = 0; i < numBack; i++) {
            sObject.sequence.add("^");
        }
        sObject.score = 0;
        beam.add(sObject);
        List<ScoreObject> maxScoreLabels;
        for (int i = 0; i < instances.length; i++) {
            maxScoreLabels = new LinkedList<>();
            for (ScoreObject so : beam) {
                List<String> prevLetter = new LinkedList<>(so.sequence.subList(so.sequence.size() - numBack, so.sequence.size()));
                Collections.reverse(prevLetter);
                MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, prevLetter);
                classifier.getClassificationScores(instances[i], scores);
                for (int j = 0; j < scores.length; j++) {
                    if(maxScoreLabels.size() < beamSize) {
                        ScoreObject s = new ScoreObject();
                        s.score = so.score + Math.log(scores[j]);
                        s.sequence = new Sequence(so.sequence);
                        s.sequence.add(states[j]);
                        maxScoreLabels.add(s);
                        Collections.sort(maxScoreLabels, new Comparator<ScoreObject>() {
                            @Override
                            public int compare(ScoreObject o1, ScoreObject o2) {
                                return o1.score > o2.score ? -1 : 1;
                            }
                        });
                    } else {
                        double score = so.score + Math.log(scores[j]);
                        for (int k = 0; k < maxScoreLabels.size(); k++) {
                            if(score > maxScoreLabels.get(k).score) {
                                ScoreObject s = new ScoreObject();
                                s.score = score;
                                s.sequence = new Sequence(so.sequence);
                                s.sequence.add(states[j]);
                                maxScoreLabels.add(k, s);
                                maxScoreLabels.remove(maxScoreLabels.size() - 1);
                                break;
                            }
                        }
                    }
                }
            }
            beam = new ArrayList<>(instances.length);
            for(ScoreObject s : maxScoreLabels) {
                beam.add(s);
            }
        }
        Sequence bestSequence = new Sequence(beam.get(0).sequence.subList(numBack, beam.get(0).sequence.size()));
        return bestSequence;
    }
}
