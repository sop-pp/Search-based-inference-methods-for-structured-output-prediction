package com.structuredoutputclassifiers.util;

import cc.mallet.types.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Marcin Dobrowolski
 */
public class MalletUtils {

    /**
     * Sets additional attributes indices that correspond to the prevSequence. Keeps the coreFeatures intact
     * @param instance
     * @param coreFeatures
     * @param prevSequence
     */
    public static void setAdditionalFeatures(Instance instance, Alphabet coreFeatures, List<String> prevSequence) {
        String[] prevLabels = new String[prevSequence.size()];
        prevLabels = prevSequence.toArray(prevLabels);
        Alphabet features = instance.getDataAlphabet();
        int addAttCount = prevLabels.length;
        FeatureVector fv = (FeatureVector) instance.getData();
        int[] indices = fv.getIndices();
        int maxIndex = 0;
        int numOfCoreFeatures = coreFeatures.size();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < numOfCoreFeatures) {
                maxIndex = i;
            } else {
                break;
            }
        }
        indices = Arrays.copyOfRange(indices, 0, maxIndex + 1);
        int counter = indices.length;
        indices = Arrays.copyOf(indices, indices.length + addAttCount);
        for (int i = 0; i < prevLabels.length; i++) {
            indices[counter] = features.lookupIndex(prevLabels[i] + i);
            counter++;
        }
        indices = Arrays.copyOf(indices, counter);
        instance.setData(new FeatureVector(features, indices));
    }

    /**
     * Converts an Instance that is internally a sequence into a InstanceList of single Instances. Changes the source,
     * which should be a List into single fields from the list according to the position of the Instance in the InstanceList.
     * Used to keep track of the string value attribute in the sentence in NER cross validation.
     * @param instanceList
     * @return
     */
    public static InstanceList splitInstanceIntoSingleInstanceListWithSource(InstanceList instanceList) {
        Alphabet dataAlphabet = instanceList.getDataAlphabet();
        LabelAlphabet targetAlphabet = (LabelAlphabet) instanceList.getTargetAlphabet();
        InstanceList result = new InstanceList(dataAlphabet, targetAlphabet);
        for (Instance instance : instanceList) {
            Instance[] instances = splitInstance(instance, dataAlphabet, targetAlphabet);
            for(int i = 0; i < instances.length; i++) {
                instances[i].setSource(((LinkedList<String>) instance.getSource()).get(i));
            }
            result.addAll(Arrays.asList(instances));
        }
        return result;
    }

    /**
     * Splits Instance that is internally a sequence into an array of single Instances
     * @param instance
     * @param dataAlphabet
     * @param targetAlphabet
     * @return
     */
    public static Instance[] splitInstance(Instance instance, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
        FeatureVectorSequence fvs = (FeatureVectorSequence) instance.getData();
        LabelSequence ls = (LabelSequence) instance.getTarget();
        Instance[] result = new Instance[ls.size()];
        for (int i = 0; i < ls.size(); i++) {
            Instance newInstance = new Instance(fvs.get(i), targetAlphabet.lookupLabel(ls.get(i)), null, null);
            newInstance.setSource(instance.getSource());
            result[i] = newInstance;
        }
        return result;
    }

    public static Sequence instanceToSequence(Instance instance) {
        Instance[] splitSequence = splitInstance(instance, instance.getDataAlphabet(),
                (LabelAlphabet) instance.getTargetAlphabet());
        Sequence result = new Sequence();
        for (Instance i : splitSequence) {
            result.add(i.getTarget().toString());
        }
        return result;
    }
}
