package com.structuredoutputclassifiers.main;

import cc.mallet.types.InstanceList;
import com.structuredoutputclassifiers.classifier.RecurrentClassifier;
import com.structuredoutputclassifiers.data.DatasetReader;
import com.structuredoutputclassifiers.data.handwriting.PixelHandwritingReader;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.mallet.cv.BasicCrossValidator;
import com.structuredoutputclassifiers.mallet.cv.CrossValidator;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.lang.reflect.InvocationTargetException;

/**
 * Author: Marcin Dobrowolski
 */
public class HandWritingClassificationExperiment extends ClassificationExperiment {



    public HandWritingClassificationExperiment(String[] args, Options options) throws ParseException {
        super(args, options);
    }

    @Override
    public void run() throws ParseException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String mode = cmd.getOptionValue("mode");
        RecurrentClassifier classifier = classifier();
        DatasetReader reader = new PixelHandwritingReader();
        InstanceListWithCoreFeatures input = reader.read(cmd.getOptionValue("input"), numBack());
        switch(cmd.getOptionValue("mode")){
            case "train-test":
                CrossValidator crossValidator = new BasicCrossValidator();
                crossValidator.setInstanceList(input);
                InstanceListWithCoreFeatures[] data = crossValidator.nextSplit();
                classifier.trainClassifier(data[0]);
                classifier.testClassifier(data[1]);
                break;
            case "cross-validation":
                classifier.crossValidation(input, 0.9, BasicCrossValidator.class, 1);
                break;
            case "small-set-cross-validation":
                classifier.crossValidation(input, 0.1, BasicCrossValidator.class, 1);
                break;
            case "single-split-train-validate-test":
                classifier.trainValidateTest(input, BasicCrossValidator.class, 0.6, 1);
                break;
            case "supplied-train-test":
                InstanceListWithCoreFeatures train = reader.read(cmd.getOptionValue("train"), numBack());
                InstanceListWithCoreFeatures test = reader.read(cmd.getOptionValue("test"), numBack());
                classifier.trainClassifier(train);
                classifier.testClassifier(test);
                break;
            default:
                System.out.println("Wrong classifier specified");
        }
    }


}
