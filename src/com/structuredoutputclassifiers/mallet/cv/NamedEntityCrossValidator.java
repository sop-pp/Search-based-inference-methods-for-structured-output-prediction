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
public class NamedEntityCrossValidator extends CrossValidator {
    private Alphabet coreFeatures;
    private Alphabet[] dictionaryFeaturesForBuckets = new Alphabet[10];

    private InstanceList[] buckets;
    private int[] splitIndices;

    public NamedEntityCrossValidator() {
        super();
        splitIndices = new int[2];
        setTrainingSetSizeProportions(0.9);
    }

    private void fillDictionaryFeaturesForBuckets() {
        for (int i = 0; i < 10; i++) {
            Alphabet alphabet = new Alphabet();
            InstanceList instances = MalletUtils.splitInstanceIntoSingleInstanceListWithSource(buckets[i]);
            for (int j = 0; j < instances.size(); j++) {
                String string = null;
                try {
                    string = (String) instances.get(j).getSource();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                alphabet.lookupIndex(string);
                alphabet.lookupIndex((string.length() < 3 ? string : string.substring(0, 3)) + "-prefix");
                alphabet.lookupIndex((string.length() < 3 ? string : string.substring(string.length() - 3, string.length())) + "-suffix");
            }
            dictionaryFeaturesForBuckets[i] = alphabet;
        }
    }

    private Alphabet addBucketFeatures(Alphabet[] bucketFeatures) {
        Alphabet alphabet = new Alphabet();
        for (Alphabet bucketAlphabet : bucketFeatures) {
            for (int i = 0; i < bucketAlphabet.size(); i++) {
                alphabet.lookupIndex(bucketAlphabet.lookupObject(i));
            }
        }
        return alphabet;
    }

    @Override
    public InstanceListWithCoreFeatures[] nextSplit() {
        InstanceListWithCoreFeatures trainingSet;
        InstanceListWithCoreFeatures testSet;
        int j = 0;
        Alphabet[] trainingBucketFeatures = new Alphabet[(int) (trainingSetSizeProportion * 10)];
        for (int i = 0; i < 10; i++) {
            if (i >= splitIndices[0] && i < splitIndices[1]) {
                continue;
            } else if (splitIndices[0] > splitIndices[1] && (i >= splitIndices[0] || i < splitIndices[1])) {
                continue;
            }
            trainingBucketFeatures[j] = dictionaryFeaturesForBuckets[i];
            j++;
        }
        LabelAlphabet labelAlphabet = (LabelAlphabet) buckets[0].getTargetAlphabet();
        Alphabet dictionaryFeatures = addBucketFeatures(trainingBucketFeatures);
        Alphabet additionalFeatures = buckets[0].getDataAlphabet();
        Alphabet allFeatures = addBucketFeatures(new Alphabet[]{coreFeatures, dictionaryFeatures, additionalFeatures});
        Alphabet coreAndDictionaryFeatures = addBucketFeatures(new Alphabet[]{coreFeatures, dictionaryFeatures});
        trainingSet = new InstanceListWithCoreFeatures(allFeatures, buckets[0].getTargetAlphabet(), coreAndDictionaryFeatures, additionalFeatures);
        testSet = new InstanceListWithCoreFeatures(allFeatures, buckets[0].getTargetAlphabet(), coreAndDictionaryFeatures, additionalFeatures);
        InstanceListWithCoreFeatures setToGrow;
        for (int i = 0; i < buckets.length; i++) {
            if (i >= splitIndices[0] && i < splitIndices[1]) {
                setToGrow = testSet;
            } else if (splitIndices[0] > splitIndices[1] && (i >= splitIndices[0] || i < splitIndices[1])) {
                setToGrow = testSet;
            } else {
                setToGrow = trainingSet;
            }
            for (Instance instanceSequence : buckets[i]) {
                Instance[] instances = MalletUtils.splitInstance(instanceSequence, allFeatures, labelAlphabet);
                FeatureVectorSequence data;
                FeatureVector[] featureVectorArray = new FeatureVector[instances.length];
                LabelSequence labelSequence = new LabelSequence(instances[0].getTargetAlphabet());
                Alphabet oldDataAlphabet = instances[0].getDataAlphabet();
                List<String> sourceSequence = null;
                for (int k = 0; k < instances.length; k++) {
                    Instance instance = instances[k];
                    int[] indices = new int[allFeatures.size()];
                    int maxIndex = 0;
                    FeatureVector oldFv = (FeatureVector) instance.getData();
                    int[] oldIndices = oldFv.getIndices();
                    for (int index : oldIndices) {
                        String value = (String) oldDataAlphabet.lookupObject(index);
                        int newIndex = allFeatures.lookupIndex(value, false);
                        indices[maxIndex] = newIndex;
                        maxIndex++;
                    }
                    if(sourceSequence == null) {
                        sourceSequence = (LinkedList<String>) instance.getSource();
                    }
                    String string = sourceSequence.get(k);
                    int index = 0;
                    index = allFeatures.lookupIndex(string, false);
                    if (index != -1) {
                        indices[maxIndex] = index;
                        maxIndex++;
                    }
                    index = allFeatures.lookupIndex((string.length() < 3 ? string : string.substring(0, 3)) + "-prefix", false);
                    if (index != -1) {
                        indices[maxIndex] = index;
                        maxIndex++;
                    }
                    index = allFeatures.lookupIndex((string.length() < 3 ? string : string.substring(string.length() - 3, string.length())) + "-suffix", false);
                    if (index != -1) {
                        indices[maxIndex] = index;
                        maxIndex++;
                    }
                    indices = Arrays.copyOfRange(indices, 0, maxIndex);
                    labelSequence.add(instance.getTarget().toString());
                    FeatureVector fv = new FeatureVector(allFeatures, indices);
                    featureVectorArray[k] = fv;
                }
                data = new FeatureVectorSequence(featureVectorArray);
                Instance newInstance = new Instance(data, labelSequence, null, null);
                newInstance.setSource(sourceSequence);
                setToGrow.add(newInstance);
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
        fillDictionaryFeaturesForBuckets();
    }

    @Override
    public void setTrainingSetSizeProportions(double trainingSetSizeProportion) {
        super.setTrainingSetSizeProportions(trainingSetSizeProportion);
        splitIndices[0] = new Random(seed).nextInt(10);
        int offset = (int) (10 - trainingSetSizeProportion * 10);
        splitIndices[1] = (splitIndices[0] + offset) % 10;
    }
}
