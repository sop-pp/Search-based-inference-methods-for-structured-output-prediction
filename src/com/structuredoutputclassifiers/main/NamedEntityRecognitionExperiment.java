package com.structuredoutputclassifiers.main;

import cc.mallet.types.InstanceList;
import com.structuredoutputclassifiers.classifier.RecurrentClassifier;
import com.structuredoutputclassifiers.data.DatasetReader;
import com.structuredoutputclassifiers.data.ner.NamedEntityReader;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreAndDictionaryFeatures;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.mallet.cv.CrossValidator;
import com.structuredoutputclassifiers.mallet.cv.NamedEntityCrossValidator;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.lang.reflect.InvocationTargetException;

/**
 * Author: Marcin Dobrowolski
 */
public class NamedEntityRecognitionExperiment extends ClassificationExperiment {

    public NamedEntityRecognitionExperiment(String[] args, Options options) throws ParseException {
        super(args, options);
    }

    @Override
    public void run() throws ParseException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String mode = cmd.getOptionValue("mode");
        RecurrentClassifier classifier = classifier();
        classifier.setNumBack(numBack());
        DatasetReader reader = new NamedEntityReader();
        switch(cmd.getOptionValue("mode")){
            case "train-test":
                InstanceListWithCoreFeatures input = reader.read(cmd.getOptionValue("input"), numBack());
                CrossValidator crossValidator = new NamedEntityCrossValidator();
                crossValidator.setInstanceList(input);
                InstanceListWithCoreFeatures[] data = crossValidator.nextSplit();
                classifier.trainClassifier(data[0]);
                classifier.testClassifier(data[1]);
                break;
            case "cross-validation":
                input = reader.read(cmd.getOptionValue("input"));
                classifier.crossValidation(input, 0.9, NamedEntityCrossValidator.class, 1);
                break;
            case "small-set-cross-validation":
                input = reader.read(cmd.getOptionValue("input"));
                classifier.crossValidation(input, 0.1, NamedEntityCrossValidator.class, 1);
                break;
            case "single-split-train-validate-test":
                input = reader.read(cmd.getOptionValue("input"));
                classifier.trainValidateTest(input, NamedEntityCrossValidator.class, 0.6, 1);
                break;
            case "supplied-train-test":
                InstanceListWithCoreFeatures train = reader.read(cmd.getOptionValue("train"));
                InstanceListWithCoreFeatures test = reader.read(cmd.getOptionValue("test"));
                classifier.trainClassifier(train);
                classifier.testClassifier(test);
                break;
            default:
                System.out.println("Wrong mode specified");
        }
    }


}
