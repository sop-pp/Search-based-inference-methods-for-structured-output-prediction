package com.structuredoutputclassifiers.data.handwriting;

import cc.mallet.types.*;
import com.structuredoutputclassifiers.data.DatasetReader;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Marcin Dobrowolski
 */
public class IndicesHandwritingReader extends DatasetReader {

    @Override
    public InstanceListWithCoreFeatures read(String filePath, int numBack) {
        final LabelAlphabet labels = new LabelAlphabet();
        for (char c = 'a'; c <= 'z'; c++) {
            labels.lookupLabel(String.valueOf(c), true);
        }
        Alphabet features = new Alphabet();
        Alphabet coreFeatures = new Alphabet();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 8; j++) {
                features.lookupIndex("p_" + i + "_" + j);
                coreFeatures.lookupIndex("p_" + i + "_" + j);
            }
        }
        for (int i = 0; i < numBack; i++) {
            for (int j = 0; j < 26; j++) {
                features.lookupIndex(String.valueOf((char) ('a' + j)) + i);
            }
            features.lookupIndex("^" + i);
        }
        InstanceListWithCoreFeatures instanceList = new InstanceListWithCoreFeatures(features, labels, coreFeatures);
        cc.mallet.types.Instance instance;
        List<FeatureVector> featureVectorList = new LinkedList<>();
        LabelSequence labelSeq = new LabelSequence(labels);
        List<String> prev = new LinkedList<String>();
        for (int i = 0; i < numBack; i++) {
            prev.add("^");
        }
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                for (int i = 0; i < numBack; i++) {
                    prev.add("^");
                }
                String[] splitLine = line.split(" ");
                if (splitLine.length == 0) {
                    break;
                }
                int[] indices = new int[splitLine.length - 1 + 27 * numBack];
                for (int j = 1; j < splitLine.length; j++) {
                    int index = Integer.parseInt(splitLine[j].substring(1));
                    indices[j - 1] = index;
                }
                for (int j = 0; j < numBack; j++) {
                    if (prev.get(j).equals("^")) {
                        indices[splitLine.length - 1 + j] = 127 + (27 * (j + 1));
                    } else {
                        indices[splitLine.length - 1 + j] = 128 + prev.get(j).charAt(0) - 'a' + (27 * j);
                    }
                }
                if (splitLine.length <= 1) {
                    FeatureVector[] featureVectorArray = new FeatureVector[featureVectorList.size()];
                    featureVectorList.toArray(featureVectorArray);
                    FeatureVectorSequence sequence = new FeatureVectorSequence(featureVectorArray);
                    instance = new cc.mallet.types.Instance(sequence, labelSeq, null, null);
                    instanceList.add(instance);
                    labelSeq = new LabelSequence(labels);
                    featureVectorList = new LinkedList<>();
                    prev = new LinkedList<>();
                    for (int i = 0; i < numBack; i++) {
                        prev.add("^");
                    }
                } else {
                    String c = splitLine[0];
                    indices = Arrays.copyOfRange(indices, 0, splitLine.length - 1 + numBack);
                    if(!prev.isEmpty()){
                        prev.remove(prev.size() - 1);
                    }
                    prev.add(0, c);
                    labelSeq.add(c);
                    featureVectorList.add(new FeatureVector(features, indices));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return instanceList;
    }

}
