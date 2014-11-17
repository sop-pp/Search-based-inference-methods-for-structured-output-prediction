package com.structuredoutputclassifiers.classifier;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Marcin Dobrowolski
 */
public class MemmOnLogReg extends LogisticRegressionClassifier {

    @Override
    public Sequence classify(Instance instance) {
        return viterbi(instance, numBack);
    }

    private Sequence viterbi(Instance instance, int numBack) {
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        String[] labels = labelAlphabetToStringArray(labelAlphabet);
        String[] states = createViterbiStateMatrix(labels, numBack);
        Instance[] instances = splitSequence(instance, null, labelAlphabet);
        StringBuilder sb = new StringBuilder();
        List<String> list = new LinkedList<>();
        for (int i = 0; i < numBack; i++) {
            sb.append("^");
            list.add("^");
        }
        List<List<String>> prevLetters = new LinkedList<>();
        prevLetters.add(list);
        List<HashMap<List<String>, Double>> v = new LinkedList<>();
        List<HashMap<List<String>, List<String>>> paths = new LinkedList<>();
        double[] scores = new double[labelAlphabet.size()];
        for (int i = 0; i < numBack && i < instances.length; i++) {
            List<List<String>> newPrevLetters = modifyPrevLetterList(prevLetters, labels);
            HashMap<List<String>, Double> pMap = new HashMap<>();
            HashMap<List<String>, List<String>> sMap = new HashMap<>();
            for (int j = 0; j < newPrevLetters.size(); j++) {
                List<String> prevLetterItem = prevLetters.get(j % prevLetters.size());
                MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, prevLetterItem);
                classifier.getClassificationScores(instances[i], scores);

                int n = labelAlphabet.lookupIndex(newPrevLetters.get(j).get(0));
                double prevP = 0.0;
                if (i > 0) {
                    prevP = v.get(i - 1).get(prevLetterItem);
                }
                double p = log2(scores[n]) + prevP;
                List<String> state = newPrevLetters.get(j);
                pMap.put(state, p);
                sMap.put(state, prevLetterItem);
            }
            prevLetters = newPrevLetters;
            v.add(pMap);
            paths.add(sMap);
        }
        for (int i = numBack; i < instances.length; i++) {
            scores = new double[labelAlphabet.size()];
            HashMap<List<String>, Double> pMap = new HashMap<>();
            HashMap<List<String>, List<String>> sMap = new HashMap<>();
            for (int j = 0; j < states.length; j++) {
                List<List<String>> newPrevLetters = prevLetters.subList((j * scores.length) % states.length,
                        (j * scores.length) % states.length + scores.length);
                int n = labelAlphabet.lookupIndex(states[j].split(" ")[0]);
                List<String> maxState = newPrevLetters.get(0);
                double maxProb = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < newPrevLetters.size(); k++) {
                    MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, newPrevLetters.get(k));
                    classifier.getClassificationScores(instances[i], scores);
                    double p = 0.0;
                    try {
                        p = log2(scores[n]) + v.get(i - 1).get(newPrevLetters.get(k));
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    if (p > maxProb) {
                        maxProb = p;
                        maxState = newPrevLetters.get(k);
                    }
                }
                String[] splitStates = states[j].split(" ");
                List<String> stateList = new LinkedList<String>();
                for(String state : splitStates) {
                    stateList.add(state);
                }
                pMap.put(stateList, maxProb);
                sMap.put(stateList, maxState);
            }
            v.add(pMap);
            paths.add(sMap);
        }
        List<String> maxState = new LinkedList<>();
        double maxProb = Double.NEGATIVE_INFINITY;
        HashMap<List<String>, Double> pMap = v.get(v.size() - 1);
        HashMap<List<String>, List<String>> sMap = paths.get(paths.size() - 1);
        for (List<String> state : pMap.keySet()) {
            double p = pMap.get(state);
            if (p > maxProb) {
                maxProb = p;
                maxState = state;
            }
        }
        Sequence result = new Sequence();
        sb = new StringBuilder();
        for(int i = 0; i < maxState.size() && i < paths.size(); i++) {
//            sb.insert(0, maxState.get(i));
//            sb.insert(0, " ");
            result.add(0, maxState.get(i));
        }
        for (int i = paths.size() - 1; i > numBack - 1; i--) {
            maxState = paths.get(i).get(maxState);
//            sb.insert(0, maxState.get(maxState.size() - 1));
//            sb.insert(0, " ");
            result.add(0, maxState.get(maxState.size() - 1));
        }
        return result;
    }
}
