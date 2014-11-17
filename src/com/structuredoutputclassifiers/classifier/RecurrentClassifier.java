package com.structuredoutputclassifiers.classifier;

import cc.mallet.types.*;
import com.structuredoutputclassifiers.data.ClassificationOutput;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.mallet.cv.BasicCrossValidator;
import com.structuredoutputclassifiers.mallet.cv.CrossValidator;
import com.structuredoutputclassifiers.util.FMeasureCalculation;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Author: Marcin Dobrowolski
 */
public abstract class RecurrentClassifier {

    private boolean printWords = false;
    private boolean printAccuracy = true;
    private boolean measureTime = true;
    private long timeInMiliseconds = 0;
    private long trainTimeInMiliseconds = 0;

    private String[][][] results;
    private boolean fmeasure = false;

    protected int seed;
    protected int numBack;
    public List<HashMap<String, Object>> params;

    public abstract com.structuredoutputclassifiers.util.Sequence classify(Instance instance);

    public abstract void trainClassifier(InstanceListWithCoreFeatures trainingInstances);

    public void testClassifier(InstanceList testingInstances) {
        ClassificationOutput[] classificationOutputs = test(testingInstances);
        if (printWords) {
            System.out.println("CORRECT\t\tPREDICTED");
            for (ClassificationOutput classificationOutput : classificationOutputs) {
                System.out.println(String.format("%s\t\t%s", classificationOutput.getTrueValues().toString
                        (), classificationOutput.getPredictions().toString()));
            }
        }

        if (printAccuracy) {
            printAccuracy(classificationOutputs);
        }
        if (fmeasure) {
            printFMeasure(classificationOutputs);
        }
        if (measureTime) {
            printTime(classificationOutputs);
        }
    }

    private ClassificationOutput[] test(InstanceList testingInstances) {
        ClassificationOutput[] classificationOutputs = new ClassificationOutput[testingInstances.size()];
        for (int i = 0; i < testingInstances.size(); i++) {
            Instance instance = testingInstances.get(i);
            ClassificationOutput co = classificationOutput(instance);
            classificationOutputs[i] = co;
            System.out.println(i + " / " + testingInstances.size());
        }
        return classificationOutputs;
    }

    public void crossValidation(InstanceList input, double proportions, Class cvClass, int seed) {
        try {
            CrossValidator crossValidator = (CrossValidator) cvClass.getConstructors()[0].newInstance();
            crossValidator.setTrainingSetSizeProportions(proportions);
            crossValidator.setInstanceList((InstanceListWithCoreFeatures) input);
            crossValidator.setSeed(seed);
            ClassificationOutput[][] classificationOutputs = cv(crossValidator);
            if (printAccuracy) {
                double singleAccuracy = 0.0;
                double sequenceAccuracy = 0.0;
                for (int i = 0; i < 10; i++) {
                    singleAccuracy += singleLabelAccuracy(classificationOutputs[i]);
                    sequenceAccuracy += sequenceAccuracy(classificationOutputs[i]);
                }
                singleAccuracy /= 10;
                sequenceAccuracy /= 10;
                System.out.println(String.format("SINGLE ACCURACY\t\tSEQUENCE ACCURACY"));
                System.out.println(String.format("%f\t\t%f", singleAccuracy, sequenceAccuracy));
            }
            if (fmeasure) {
                FMeasureCalculation fMeasure = new FMeasureCalculation();
                for (int i = 0; i < 10; i++) {
                    FMeasureCalculation temp = fMeasureOverAllInstances(classificationOutputs[i]);
                    fMeasure.fMeasure += temp.fMeasure;
                    fMeasure.precision += temp.precision;
                    fMeasure.recall = temp.recall;
                }
                fMeasure.fMeasure /= 10;
                fMeasure.precision /= 10;
                fMeasure.recall /= 10;
                System.out.println("F-MEASURE\t\tPRECISION\t\tRECALL\t\tALL");
                System.out.println(String.format("%f\t\t%f\t\t%f", fMeasure.fMeasure, fMeasure.precision, fMeasure.recall));

                fMeasure = new FMeasureCalculation();
                for (int i = 0; i < 10; i++) {
                    FMeasureCalculation temp = averageFMeasureOverSingleInstances(classificationOutputs[i]);
                    fMeasure.fMeasure += temp.fMeasure;
                    fMeasure.precision += temp.precision;
                    fMeasure.recall = temp.recall;
                }
                fMeasure.fMeasure /= 10;
                fMeasure.precision /= 10;
                fMeasure.recall /= 10;
                System.out.println("F-MEASURE\t\tPRECISION\t\tRECALL\t\tSINGLE");
                System.out.println(String.format("%f\t\t%f\t\t%f", fMeasure.fMeasure, fMeasure.precision, fMeasure.recall));
            }
            int instanceCount = 0;
            for (ClassificationOutput[] co1 : classificationOutputs) {
                for(ClassificationOutput co : co1) {
                    instanceCount += co.sequenceSize();
                }
            }
            if (measureTime) {
                System.out.println("TOTAL TIME IN MS");
                System.out.println(timeInMiliseconds);
                System.out.println("AVERAGE TIME IN MS");
                System.out.println((double) timeInMiliseconds / (double) instanceCount);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public ClassificationOutput[][] cv(CrossValidator crossValidator) {
        ClassificationOutput[][] classificationOutputs = new ClassificationOutput[10][];
        for (int i = 0; i < 10; i++) {
            InstanceListWithCoreFeatures[] instanceLists = crossValidator.nextSplit();
            trainClassifier(instanceLists[0]);
            classificationOutputs[i] = test(instanceLists[1]);
        }
        return classificationOutputs;
    }

    public void trainValidateTest(InstanceList input, Class crossValidatorClass,
                                  double trainValidateProportions, int seed) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        CrossValidator crossValidator = (CrossValidator) crossValidatorClass.getConstructors()[0].newInstance();
        crossValidator.setTrainingSetSizeProportions(0.9);
        crossValidator.setInstanceList((InstanceListWithCoreFeatures) input);
        crossValidator.setSeed(seed);
        InstanceListWithCoreFeatures[] trainTestList = crossValidator.nextSplit();
        CrossValidator trainValidateCrossValidator = (CrossValidator) crossValidatorClass.getConstructors()[0].newInstance();
        trainValidateCrossValidator.setSeed(seed);
        List<HashMap<String, Object>> tuningParams = getTuningParams();
        double[] wordAccuracy = new double[20];
        double[] letterAccuracy = new double[20];
        ClassificationOutput[][] classificationOutputs = null;
        double maxAccuracy = 0.0;
        HashMap<String, Object> maxParams = null;
        for (HashMap<String, Object> paramSet : tuningParams) {
            setCurrentParamSet(paramSet);
            trainValidateCrossValidator.setTrainingSetSizeProportions(trainValidateProportions);
            trainValidateCrossValidator.setInstanceList(trainTestList[0]);
            classificationOutputs = cv(trainValidateCrossValidator);
            double accuracy = 0;
            for(int i = 0; i < 10; i++) {
                accuracy += sequenceAccuracy(classificationOutputs[i]);
            }
            if(accuracy > maxAccuracy) {
                maxAccuracy = accuracy;
                maxParams = paramSet;
            }
        }
        setCurrentParamSet(maxParams);
        testClassifier(trainTestList[1]);
    }

    public abstract void setCurrentParamSet(HashMap<String, Object> paramSet);

    public List<HashMap<String, Object>> getTuningParams() {
        return params;
    }

    public void setTuningParams(List<HashMap<String, Object>> params) {
        this.params = params;
    }

    private ClassificationOutput classificationOutput(Instance instance) {
        ClassificationOutput co = new ClassificationOutput();
        double start = System.currentTimeMillis();
        Sequence predictedSequence = classify(instance);
        co.setPredictedSequence(predictedSequence);
        double end = System.currentTimeMillis();
        Sequence trueSequence = MalletUtils.instanceToSequence(instance);
        trueSequence.setComparator(predictedSequence.getComparator());
        co.setTrueSequence(trueSequence);
        co.setClassificationTimeInMiliseconds(end - start);
        timeInMiliseconds += end - start;
        return co;
    }

    private void printAccuracy(ClassificationOutput[] classificationOutputs) {
        double singleAccuracy = singleLabelAccuracy(classificationOutputs);
        double sequenceAccuracy = sequenceAccuracy(classificationOutputs);
        System.out.println(String.format("SINGLE ACCURACY\t\tSEQUENCE ACCURACY"));
        System.out.println(String.format("%f\t\t%f", singleAccuracy, sequenceAccuracy));
    }

    private void printFMeasure(ClassificationOutput[] classificationOutputs) {
        FMeasureCalculation fMeasure = fMeasureOverAllInstances(classificationOutputs);
        System.out.println("F-MEASURE\t\tPRECISION\t\tRECALL ALL");
        System.out.println(String.format("%f\t\t%f\t\t%f", fMeasure.fMeasure, fMeasure.precision, fMeasure.recall));

        fMeasure = averageFMeasureOverSingleInstances(classificationOutputs);
        System.out.println("F-MEASURE\t\tPRECISION\t\tRECALL SINGLE");
        System.out.println(String.format("%f\t\t%f\t\t%f", fMeasure.fMeasure, fMeasure.precision, fMeasure.recall));
    }

    private void printTime(ClassificationOutput[] classificationOutputs) {
        System.out.println("TOTAL TIME IN MS");
        System.out.println(timeInMiliseconds);
        System.out.println("AVERAGE TIME IN MS");
        System.out.println((double) timeInMiliseconds / (double) classificationOutputs.length);
    }

    private FMeasureCalculation averageFMeasureOverSingleInstances(ClassificationOutput[] classificationOutputs) {
        FMeasureCalculation result = new FMeasureCalculation();
        double precisionSum = 0.0;
        double recallSum = 0.0;
        double fMeasureSum = 0.0;
        for (ClassificationOutput classificationOutput : classificationOutputs) {
            precisionSum += classificationOutput.precision();
            recallSum += classificationOutput.recall();
            fMeasureSum += classificationOutput.fMeasure();
        }
        result.precision = precisionSum / classificationOutputs.length;
        result.recall = recallSum / classificationOutputs.length;
        result.fMeasure = fMeasureSum / classificationOutputs.length;
        return result;
    }


    protected FMeasureCalculation fMeasureOverAllInstances(ClassificationOutput[] classificationOutputs) {
        FMeasureCalculation result = new FMeasureCalculation();
        result.precision = precision(classificationOutputs);
        result.recall = recall(classificationOutputs);
        result.fMeasure = (2.0 * result.precision * result.recall) / (result.precision + result.recall);
        return result;
    }

    protected double precision(ClassificationOutput[] classificationOutputs) {
        double tp = 0;
        double fp = 0;
        for (ClassificationOutput classificationOutput : classificationOutputs) {
            Sequence predictions = classificationOutput.getPredictions();
            Sequence trueValues = classificationOutput.getTrueValues();
            for (int j = 0; j < classificationOutput.sequenceSize(); j++) {
                boolean isCorrectEntity = isEntity(trueValues.get(j));
                boolean isPredictedEntity = isEntity(predictions.get(j));
                if (isCorrectEntity && isPredictedEntity) {
                    tp++;
                } else if (!isCorrectEntity && isPredictedEntity) {
                    fp++;
                }
            }
        }
        return tp + fp == 0 ? 0 : tp / (tp + fp);
    }

    protected double recall(ClassificationOutput[] classificationOutputs) {
        double tp = 0;
        double fn = 0;
        for (ClassificationOutput classificationOutput : classificationOutputs) {
            Sequence predictions = classificationOutput.getPredictions();
            Sequence trueValues = classificationOutput.getTrueValues();
            for (int j = 0; j < classificationOutput.sequenceSize(); j++) {
                boolean isCorrectEntity = isEntity(trueValues.get(j));
                boolean isPredictedEntity = isEntity(predictions.get(j));
                if (isPredictedEntity && isCorrectEntity) {
                    tp++;
                } else if (!isPredictedEntity && isCorrectEntity) {
                    fn++;
                }
            }
        }
        return tp + fn == 0 ? 0 : tp / (tp + fn);
    }

    protected boolean isEntity(String entity) {
        return !entity.equals("O") && !entity.equals("0");
    }

    protected double sequenceAccuracy(ClassificationOutput[] classificationOutputs) {
        int correct = 0;
        for (ClassificationOutput classificationOutput : classificationOutputs) {
            if (classificationOutput.isCorrectSequence()) {
                correct++;
            }
        }
        return (double) correct / (double) classificationOutputs.length;
    }

    protected double singleLabelAccuracy(ClassificationOutput[] classificationOutputs) {
        int correct = 0;
        int all = 0;
        for (ClassificationOutput classificationOutput : classificationOutputs) {
            all += classificationOutput.sequenceSize();
            correct += classificationOutput.correctSinglePredictions();
        }
        return (double) correct / (double) all;
    }

    protected List<List<String>> modifyPrevLetterList(List<List<String>> prevLetters, String[] labels) {
        List<List<String>> result;
        List<List<String>> stateList = new LinkedList<>();
        for (int i = 0; i < prevLetters.size(); i++) {
            List<String> l = prevLetters.get(i);
            l = l.subList(0, l.size() - 1);
            stateList.add(l);
        }
        List<List<String>> tempStateList = new LinkedList<>(stateList.subList(0, stateList.size()));
        for (int i = 0; i < labels.length - 1; i++) {
            stateList.addAll(tempStateList);
        }
        for (int i = 0; i < stateList.size(); i++) {
            String prefix = labels[(i / prevLetters.size()) % labels.length];
            List<String> l = new LinkedList<String>(stateList.get(i));
            l.add(0, prefix);
            stateList.set(i, l);
        }
        result = new ArrayList<>(stateList);
        return result;
    }

    protected String[] labelAlphabetToStringArray(LabelAlphabet alphabet) {
        Object[] labels = alphabet.toArray();
        String[] strings = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            strings[i] = ((String) labels[i]);
        }
        return strings;
    }

    protected InstanceList splitSequenceIntoSingleInstanceList(InstanceList instanceList) {
        Alphabet dataAlphabet = instanceList.getDataAlphabet();
        LabelAlphabet targetAlphabet = (LabelAlphabet) instanceList.getTargetAlphabet();
        InstanceList result = new InstanceList(dataAlphabet, targetAlphabet);
        for (Instance instance : instanceList) {
            result.addAll(Arrays.asList(splitSequence(instance, dataAlphabet, targetAlphabet)));
        }
        return result;
    }

    protected Instance[] splitSequence(Instance instance, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
        FeatureVectorSequence fvs = (FeatureVectorSequence) instance.getData();
        LabelSequence ls = (LabelSequence) instance.getTarget();
        Instance[] result = new Instance[ls.size()];
        for (int i = 0; i < ls.size(); i++) {
            Instance newInstance = new Instance(fvs.get(i), targetAlphabet.lookupLabel(ls.get(i)), null, null);
            result[i] = newInstance;
        }
        return result;
    }

    protected InstanceList[] splitSequencesIntoSingleInstanceLists(InstanceList instanceList, Random r,
                                                                   double[] proportions) {
        instanceList.shuffle(r);
        InstanceList[] instanceLists = new InstanceList[proportions.length];
        double[] maxInd = proportions.clone();
        MatrixOps.normalize(maxInd);
        Alphabet dataAlphabet = instanceList.getDataAlphabet();
        LabelAlphabet targetAlphabet = (LabelAlphabet) instanceList.getTargetAlphabet();
        for (int i = 0; i < maxInd.length; i++) {
            instanceLists[i] = new InstanceList(instanceList.getDataAlphabet(), instanceList.getTargetAlphabet());
            if (i > 0) {
                maxInd[i] += maxInd[i - 1];
            }
        }
        for (int i = 0; i < maxInd.length; i++) {
            maxInd[i] = Math.rint(maxInd[i] * instanceList.size());
        }
        for (int i = 0, j = 0; i < instanceList.size(); i++) {
            while (i >= maxInd[j] && j < instanceLists.length)
                j++;
            Instance instance = instanceList.get(i);
            for (Instance newInstance : splitSequence(instance, dataAlphabet, targetAlphabet)) {
                instanceLists[j].add(newInstance);
            }
        }
        return instanceLists;
    }

    protected HashMap<String, HashMap<String, Double>> createTransitionProbabilitiesMatrix(InstanceList instanceList) {
        LabelAlphabet targetAlphabet = (LabelAlphabet) instanceList.getTargetAlphabet();
        String[] states = labelAlphabetToStringArray(targetAlphabet);
        HashMap<String, HashMap<String, Integer>> countMap = new HashMap<>();
        HashMap<String, HashMap<String, Double>> probMap = new HashMap<>();
        String prev = "^";
        for (Instance instanceSequence : instanceList) {
            Instance[] instances = splitSequence(instanceSequence, null, targetAlphabet);
            for (Instance instance : instances) {
                String s = ((Label) instance.getTarget()).toString();
                if (!countMap.containsKey(prev)) {
                    countMap.put(prev, new HashMap<String, Integer>());
                }
                Integer integer = countMap.get(prev).get(s);
                if (integer == null) {
                    countMap.get(prev).put(s, 1);
                    continue;
                }
                countMap.get(prev).put(s, integer + 1);
                prev = s;
            }
            prev = "^";
        }
        for (String prevKey : countMap.keySet()) {
            HashMap<String, Integer> map = countMap.get(prevKey);
            HashMap<String, Double> pMap = new HashMap<>();
            int sum = 0;
            for (String nextKey : map.keySet()) {
                sum += map.get(nextKey);
            }
            for (String state : states) {
                double p = 0;
                if (map.containsKey(state)) {
                    p = (map.get(state) + 1.0) / (sum + 1.0);
                } else {
                    p = 1.0 / (sum + 1.0);
                }
                pMap.put(state, p);
            }
            probMap.put(prevKey, pMap);
        }
        return probMap;
    }

    protected String buildWordFromSequenceInstance(Instance instance) {
        Instance[] instances = splitSequence(instance, null, (LabelAlphabet) instance.getTargetAlphabet());
        StringBuilder sb = new StringBuilder();
        for (Instance iterInstance : instances) {
            sb.append(iterInstance.getTarget().toString());
        }
        return sb.toString();
    }

    protected String[] createViterbiStateMatrix(String[] labels, int numBack) {
        String[] states = new String[(int) Math.pow(labels.length, numBack)];
        ArrayList<String> stateList = new ArrayList<>(Arrays.asList(labels));
        for (int i = 1; i < numBack; i++) {
            ArrayList<String> tempStateList = new ArrayList<>(stateList.subList(0, stateList.size()));
            for (int j = 0; j < labels.length - 1; j++) {
                stateList.addAll(tempStateList);
            }
            for (int j = 0; j < stateList.size(); j++) {
                String s = stateList.get(j);
                String prefix = labels[j / ((int) Math.pow(labels.length, i))];
                s = prefix + " " + s;
//                s = prefix + s;
                stateList.set(j, s);
            }
        }
        stateList.toArray(states);
        return states;
    }

    protected double log2(double a) {
        return Math.log(a) / Math.log(2);
    }

    public boolean isPrintWords() {
        return printWords;
    }

    public void setPrintWords(boolean printWords) {
        this.printWords = printWords;
    }

    public boolean isPrintAccuracy() {
        return printAccuracy;
    }

    public void setPrintAccuracy(boolean printAccuracy) {
        this.printAccuracy = printAccuracy;
    }

    public boolean isMeasureTime() {
        return measureTime;
    }

    public void setMeasureTime(boolean measureTime) {
        this.measureTime = measureTime;
    }

    public boolean isFmeasure() {
        return fmeasure;
    }

    public void setFmeasure(boolean fmeasure) {
        this.fmeasure = fmeasure;
    }

    public int getSeed() {
        return this.seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getNumBack() {
        return this.numBack;
    }

    public void setNumBack(int numBack) {
        this.numBack = numBack;
    }
}
