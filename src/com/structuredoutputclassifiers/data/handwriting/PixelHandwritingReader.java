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
public class PixelHandwritingReader extends DatasetReader {

    @Override
    public InstanceListWithCoreFeatures read(String filePath, int numBack) {
        final LabelAlphabet labels = new LabelAlphabet();
        for (char c = 'a'; c <= 'z'; c++) {
            labels.lookupLabel(String.valueOf(c), true);
        }
        Alphabet features = new Alphabet();
        Alphabet coreFeatures = new Alphabet();
        Alphabet additionalFeatures = new Alphabet();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 8; j++) {
                features.lookupIndex("p_" + i + "_" + j);
                coreFeatures.lookupIndex("p_" + i + "_" + j);
            }
        }
        for (int i = 0; i < numBack; i++) {
            for (int j = 0; j < 26; j++) {
                features.lookupIndex(String.valueOf((char) ('a' + j)) + i);
                additionalFeatures.lookupIndex(String.valueOf((char) ('a' + j)) + i);
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
                String[] splitLine = line.split("\t");
                if (splitLine.length == 0) {
                    break;
                }
                int[] indices = new int[128 + 27 * numBack];
                int counter = 0;
                for (int j = 6; j < splitLine.length; j++) {
                    if (splitLine[j].equals("1")) {
                        indices[counter] = j - 6;
                        counter++;
                    }
                }
                for (int j = 0; j < numBack; j++) {
                    if (prev.get(j).equals("^")) {
                        indices[counter] = 127 + (27 * (j + 1));
                        counter++;
                    } else {
                        indices[counter] = 128 + prev.get(j).charAt(0) - 'a' + (27 * j);
                        counter++;
                    }
                }
                indices = Arrays.copyOf(indices, counter);
                String c = splitLine[1];
                if(numBack > 0) {
                    prev.remove(prev.size() - 1);
                    prev.add(0, c);
                }
                labelSeq.add(c);
                featureVectorList.add(new FeatureVector(features, indices));
                if (Integer.parseInt(splitLine[2]) == -1) {
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
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instanceList;
    }
}
