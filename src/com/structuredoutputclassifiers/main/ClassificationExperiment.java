package com.structuredoutputclassifiers.main;

import com.structuredoutputclassifiers.classifier.*;
import org.apache.commons.cli.*;

import java.lang.reflect.InvocationTargetException;

/**
 * Author: Marcin Dobrowolski
 */
public abstract class ClassificationExperiment {
    protected Options options;
    protected CommandLine cmd;
    protected CommandLineParser cmdParser;
    protected String[] args;

    protected ClassificationExperiment(String[] args, Options options) throws ParseException {
        this.options = options;
        this.args = args;
        initCmd();
        cmdParser = new BasicParser();
        cmd = cmdParser.parse(options, args, true);
    }

    protected void initCmd() {
        Option mode = OptionBuilder.withArgName("mode")
                .hasArg()
                .create("mode");
        options.addOption(mode);

        String modeValue;

        // TODO Catch org.apache.commons.cli.MissingOptionException: Missing required option

        Option inputOption = OptionBuilder.withArgName("filepath")
                .hasArg()
                .create("input");
        Option trainOption = OptionBuilder.withArgName("filepath")
                .hasArg()
                .create("train");
        Option validateOption = OptionBuilder.withArgName("filepath")
                .hasArg()
                .create("validate");
        Option testOption = OptionBuilder.withArgName("filepath")
                .hasArg()
                .create("test");
        Option outputOption = OptionBuilder.withArgName("filepath")
                .hasArg()
                .create("output");
        Option classifierOption = OptionBuilder.withArgName("classifier")
                .hasArg()
                .create("classifier");
        Option trainTestOption = OptionBuilder.withArgName("mode")
                .hasArg()
                .create("mode");
        Option verboseOption = OptionBuilder.create("verbose");
        Option numbackOption = OptionBuilder.hasArg().create("numback");
        Option wekaClassifierOptionsOption = OptionBuilder.hasArg().create("weka");
        Option beamSizeOption = OptionBuilder.hasArg().create("beamsize");
        Option sampleSizeOption = OptionBuilder.hasArg().create("samplesize");
        Option epsilonOption = OptionBuilder.hasArg().create("epsilon");

        options.addOption(inputOption);
        options.addOption(trainOption);
        options.addOption(validateOption);
        options.addOption(testOption);
        options.addOption(outputOption);
        options.addOption(classifierOption);
        options.addOption(trainTestOption);
        options.addOption(verboseOption);
        options.addOption(numbackOption);
        options.addOption(wekaClassifierOptionsOption);
        options.addOption(beamSizeOption);
        options.addOption(sampleSizeOption);
        options.addOption(epsilonOption);
    }

    protected void getOption(Option option) throws ParseException {
        options.addOption(option);
        cmd = cmdParser.parse(options, args, true);
    }

    protected Integer numBack() {
        return Integer.valueOf(cmd.getOptionValue("numback"));
    }

    protected Integer beamSize() {
        return Integer.valueOf(cmd.getOptionValue("beamsize"));
    }

    protected Integer sampleSize() {
        return Integer.valueOf(cmd.getOptionValue("samplesize"));
    }

    protected Double epsilon() { return Double.valueOf(cmd.getOptionValue("epsilon")); }

    protected String[] wekaClassifierOptions() throws ParseException {
        return cmd.getOptionValue("weka").split(" ");
    }

    protected RecurrentClassifier classifier() throws ParseException {
        switch(cmd.getOptionValue("classifier")) {
            case "memm":
                return new MemmOnLogReg();
            case "naive-hmm":
                return new NaiveHMM();
            case "beam-search":
                return new BeamSearchClassifier(beamSize());
            case "uniform-cost-search":
                return new UniformCostSearchClassifier(epsilon());
            case "f-measure-maximizer":
                return new MonteCarloFMeasureMaximizer(sampleSize());
            case "svm":
                return new LibSVMClassifierWrapper(wekaClassifierOptions());
            case "log-reg":
                return new LogisticRegressionClassifier();
        }
        return null;
    }

    public abstract void run() throws ParseException, IllegalAccessException, InstantiationException, InvocationTargetException;
}
