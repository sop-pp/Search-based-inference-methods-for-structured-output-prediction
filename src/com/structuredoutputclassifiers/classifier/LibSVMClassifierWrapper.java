package com.structuredoutputclassifiers.classifier;

import cc.mallet.types.*;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a wrapper for the LibSVM classifier used in Weka. The purpose of the wrapper is to enable
 * the use of Mallet Instance classes to hold the data, as they are much 'frendlier' when it comes to holding
 * data of a sequential nature.
 */

public class LibSVMClassifierWrapper extends RecurrentClassifier {

    private int numBack;
    private Alphabet coreFeatures;
    private final String[] wekaClassifierOptions;
    private LibSVM libSVM;

    public LibSVMClassifierWrapper(String[] wekaClassifierOptions) {
        this.wekaClassifierOptions = wekaClassifierOptions;
    }

    @Override
    public Sequence classify(Instance instance) {
        Sequence sequence = new Sequence();
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        Instance[] instances = splitSequence(instance, null, labelAlphabet);
        int numOfAttributesPlusLabel = instance.getDataAlphabet().size() + 1;
        FastVector attributes = new FastVector();
        FastVector labels = new FastVector();
        FastVector binaryVector = new FastVector();
        binaryVector.addElement("0");
        binaryVector.addElement("1");
        for(int i = 0; i < labelAlphabet.size(); i++) {
            labels.addElement(labelAlphabet.lookupLabel(i).toString());
        }
        attributes.addElement(new Attribute("Label", labels));
        Alphabet dataAlphabet = instance.getDataAlphabet();
        for (int i = 0; i < dataAlphabet.size(); i++) {
            String attributeName = (String) dataAlphabet.lookupObject(i);
            attributes.addElement(new Attribute(attributeName, binaryVector));
        }
        Instances wekaTestingSet = new Instances("Testing set", attributes, instances.length);
        wekaTestingSet.setClassIndex(0);

        List<String> prevLabels = new LinkedList<>();
        for(int i = 0; i < numBack; i++) {
            prevLabels.add("^");
        }
        for(Instance inst : instances) {
            MalletUtils.setAdditionalFeatures(inst, coreFeatures, prevLabels);
            double[] attValues = new double[numOfAttributesPlusLabel];
            Arrays.fill(attValues, 0.0);
            int[] indices = ((FeatureVector) inst.getData()).getIndices();
            for (int index : indices) {
                attValues[index + 1] = 1.0;
            }
            weka.core.Instance wekaInstance = new SparseInstance(1.0, attValues);
            wekaInstance.setDataset(wekaTestingSet);
            wekaInstance.setClassMissing();
            wekaTestingSet.add(wekaInstance);
            try {
                double[] distribution = libSVM.distributionForInstance(wekaInstance);
                double classifiedIndex = MatrixOps.maxIndex(distribution);
                String label = labelAlphabet.lookupLabel((int) classifiedIndex).toString();
                sequence.add(label);
                prevLabels.add(label);
                prevLabels.remove(0);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return sequence;
    }

    @Override
    public void setCurrentParamSet(HashMap<String, Object> paramSet) {
        throw new NotImplementedException();
    }

    @Override
    public List<HashMap<String, Object>> getTuningParams() {
        throw new NotImplementedException();
    }

    @Override
    public void trainClassifier(InstanceListWithCoreFeatures trainingInstances) {
        double start = System.currentTimeMillis();
        InstanceList instances = splitSequenceIntoSingleInstanceList(trainingInstances);
        int numOfAttributesPlusLabel = instances.getDataAlphabet().size() + 1;
        FastVector attributes = new FastVector();
        FastVector labels = new FastVector();
        FastVector binaryVector = new FastVector();
        binaryVector.addElement("0");
        binaryVector.addElement("1");
        LabelAlphabet labelAlphabet = (LabelAlphabet) instances.getTargetAlphabet();
        for(int i = 0; i < labelAlphabet.size(); i++) {
            labels.addElement(labelAlphabet.lookupLabel(i).toString());
        }
        attributes.addElement(new Attribute("Label", labels));
        Alphabet dataAlphabet = instances.getDataAlphabet();
        for (int i = 0; i < dataAlphabet.size(); i++) {
            String attributeName = (String) dataAlphabet.lookupObject(i);
            attributes.addElement(new Attribute(attributeName, binaryVector));
        }
        Instances wekaTrainingSet = new Instances("Training set", attributes, instances.size());
        wekaTrainingSet.setClassIndex(0);
        for (Instance instance : instances) {
            double[] attValues = new double[numOfAttributesPlusLabel];
            Label label = (Label) instance.getTarget();
            Arrays.fill(attValues, 0.0);
            int[] indices = ((FeatureVector) instance.getData()).getIndices();
            attValues[0] = labels.indexOf(label.toString());
            for (int index : indices) {
                attValues[index + 1] = 1.0;
            }
            weka.core.Instance wekaInstance = new SparseInstance(1.0, attValues);
            wekaInstance.setDataset(wekaTrainingSet);
            wekaTrainingSet.add(wekaInstance);
        }
        try {
            libSVM.buildClassifier(wekaTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        double end = System.currentTimeMillis();
        System.out.println("BUILD TIME IN MS");
        System.out.println(end - start);
    }

    public void trainClassifier(InstanceListWithCoreFeatures trainingInstances, double param) {

    }
}
