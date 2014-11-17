package com.structuredoutputclassifiers.data.ner;

import cc.mallet.types.*;
import com.structuredoutputclassifiers.data.DatasetReader;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;

import java.io.*;
import java.util.*;

/**
 * Author: Marcin Dobrowolski
 */
public class NamedEntityReader extends DatasetReader {

    private String[] entities = {"LOC", "MISC", "ORG", "PER"};

    public NamedEntityReader() {

    }

    @Override
    public InstanceListWithCoreFeatures read(String filePath, int numBack) {
        final LabelAlphabet labels = new LabelAlphabet();
        for (String entity : entities) {
            labels.lookupLabel("B-" + entity);
            labels.lookupLabel("I-" + entity);
        }
        labels.lookupLabel("O");
        Alphabet features = new Alphabet();
        Alphabet coreFeatures = new Alphabet();
        Alphabet additionalFeatures = new Alphabet();
        features.lookupIndex("case");
        coreFeatures.lookupIndex("case");
        for (int i = 0; i < numBack; i++) {
            for (String entity : entities) {
                features.lookupIndex("B-" + entity + i);
                additionalFeatures.lookupIndex("B-" + entity + i);
                features.lookupIndex("I-" + entity + i);
                additionalFeatures.lookupIndex("I-" + entity + i);
            }
            features.lookupIndex("O" + i);
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
            String line = br.readLine();
            int z = 0;
            List<String> entities = new LinkedList<String>();
            while (line != null) {
                z++;
                for (int i = 0; i < numBack; i++) {
                    prev.add("^");
                }
                String[] splitLine = line.split(" ");
                if (splitLine.length <= 1) {
                    FeatureVector[] featureVectorArray = new FeatureVector[featureVectorList.size()];
                    featureVectorList.toArray(featureVectorArray);
                    FeatureVectorSequence sequence = new FeatureVectorSequence(featureVectorArray);
                    instance = new cc.mallet.types.Instance(sequence, labelSeq, null, null);
                    instance.setSource(entities);
                    entities = new LinkedList<String>();
                    instanceList.add(instance);
                    labelSeq = new LabelSequence(labels);
                    featureVectorList = new LinkedList<>();
                    prev = new LinkedList<>();
                    for (int i = 0; i < numBack; i++) {
                        prev.add("^");
                    }
                    line = br.readLine();
                    continue;
                }
                String string = splitLine[0];
                entities.add(string);
                String entity = splitLine[splitLine.length - 1];
                labelSeq.add(entity);
                int[] indices = new int[1 + numBack];
                int i = 0;
                if(!string.toLowerCase().equals(string)) {
                    indices[0] = 0;
                    i++;
                }
                for(int j = 0; j < numBack; j++) {
                    indices[i] = features.lookupIndex(prev.get(j) + j);
                    i++;
                }

                indices = Arrays.copyOfRange(indices, 0, i);
                featureVectorList.add(new FeatureVector(features, indices));
                if(numBack > 0) {
                    prev.remove(prev.size() - 1);
                    prev.add(0, entity);
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instanceList;
    }

    public InstanceList[] getSequenceInstanceListWithPreviousItemsAndDictionaryAttributes(String filePath, int numBack) {
        Set<String> words = new LinkedHashSet<>();
        Set<String> prefixes = new LinkedHashSet<>();
        Set<String> suffixes = new LinkedHashSet<>();
        int numLines = 0;
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                numLines++;
                String[] splitLine = line.split(" ");
                String string = splitLine[0];
                words.add(string);
                prefixes.add((string.length() < 3 ? string : string.substring(0, 3)) + "-prefix");
                suffixes.add((string.length() < 3 ? string : string.substring(string.length() - 3, string.length())) + "-suffix");
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final LabelAlphabet labels = new LabelAlphabet();
        for (String entity : entities) {
            labels.lookupLabel("B-" + entity);
            labels.lookupLabel("I-" + entity);
        }
        int ninetyPercent = (int) (0.9 * numLines);
        labels.lookupLabel("O");
        Alphabet features = new Alphabet();
        Alphabet coreFeatures = new Alphabet();
        Alphabet additionalFeatures = new Alphabet();
        features.lookupIndex("case");
        coreFeatures.lookupIndex("case");
        int i = 0;
        for(String featureString : words) {
            if(i < ninetyPercent) {
                features.lookupIndex(featureString);
                coreFeatures.lookupIndex(featureString);
            }
        }
        i = 0;
        for(String featureString : prefixes) {
            if(i < ninetyPercent) {
                features.lookupIndex(featureString);
                coreFeatures.lookupIndex(featureString);
            }
        }
        i = 0;
        for(String featureString : suffixes) {
            if(i < ninetyPercent) {
                features.lookupIndex(featureString);
                coreFeatures.lookupIndex(featureString);
            }
        }
        for (i = 0; i < numBack; i++) {
            for (String entity : entities) {
                features.lookupIndex("B-" + entity + i);
                additionalFeatures.lookupIndex("B-" + entity + i);
                features.lookupIndex("I-" + entity + i);
                additionalFeatures.lookupIndex("I-" + entity + i);
            }
            features.lookupIndex("O" + i);
            features.lookupIndex("^" + i);
        }
        InstanceListWithCoreFeatures[] instanceLists = new InstanceListWithCoreFeatures[2];
        instanceLists[0] = new InstanceListWithCoreFeatures(features, labels, coreFeatures);
        instanceLists[1] = new InstanceListWithCoreFeatures(features, labels, coreFeatures);
        cc.mallet.types.Instance instance;
        List<FeatureVector> featureVectorList = new LinkedList<>();
        LabelSequence labelSeq = new LabelSequence(labels);
        List<String> prev = new LinkedList<String>();
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int num = 0;
            while ((line = br.readLine()) != null) {
                for (i = 0; i < numBack; i++) {
                    prev.add("^");
                }
                String[] splitLine = line.split(" ");
                if (splitLine.length <= 1) {
                    FeatureVector[] featureVectorArray = new FeatureVector[featureVectorList.size()];
                    featureVectorList.toArray(featureVectorArray);
                    FeatureVectorSequence sequence = new FeatureVectorSequence(featureVectorArray);
                    instance = new cc.mallet.types.Instance(sequence, labelSeq, null, null);
                    if(num < ninetyPercent) {
                        instanceLists[0].add(instance);
                    } else {
                        instanceLists[1].add(instance);
                    }
                    labelSeq = new LabelSequence(labels);
                    featureVectorList = new LinkedList<>();
                    prev = new LinkedList<>();
                    for (i = 0; i < numBack; i++) {
                        prev.add("^");
                    }
                    continue;
                }
                String string = splitLine[0];
                String entity = splitLine[splitLine.length - 1];
                labelSeq.add(entity);
                int[] indices = new int[4 + numBack];
                double[] values = new double[features.size() + numBack];
                i = 0;
                if(!string.toLowerCase().equals(string)) {
                    indices[0] = 0;
                    i++;
                }
                int index = features.lookupIndex(string, false);
                if(index != -1) {
                    indices[i] = index;
                    i++;
                }
                index = features.lookupIndex((string.length() < 3 ? string : string.substring(0, 3)) + "-prefix");
                if(index != -1) {
                    indices[i] = index;
                    i++;
                }
                index = features.lookupIndex((string.length() < 3 ? string : string.substring(string.length() - 3, string.length())) + "-suffix");
                if(index != -1) {
                    indices[i] = index;
                    i++;
                }
                for(int j = 0; j < numBack; i++, j++) {
                    indices[i] = features.lookupIndex(prev.get(j) + j);
                }
                indices = Arrays.copyOfRange(indices, 0, i);
                featureVectorList.add(new FeatureVector(features, indices));
                num++;
//                prev.remove(prev.size() - 1);
//                prev.add(0, entity);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instanceLists;
    }
}
