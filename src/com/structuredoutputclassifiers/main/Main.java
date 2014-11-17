package com.structuredoutputclassifiers.main;

import org.apache.commons.cli.*;

/**
 * Author: Marcin Dobrowolski
 */
public class Main {
    public static void main(String[] args) throws Exception {
        try {
            if (args == null || args.length == 0) {
                throw new ParseException("No arguments");
            }
            CommandLineParser parser = new BasicParser();
            Options options = new Options();
            Option module = OptionBuilder.withArgName("module")
                    .hasArg()
                    .create("module");
            options.addOption(module);
            CommandLine cmd = parser.parse(options, args, true);
            String moduleValue = cmd.getOptionValue("module");
            ClassificationExperiment experiment = null;

            if ("hw".equals(moduleValue)) {
                experiment = new HandWritingClassificationExperiment(args, options);
            } else if ("ner".equals(moduleValue)) {
                experiment = new NamedEntityRecognitionExperiment(args, options);
            }

            if (experiment != null){
                experiment.run();
            } else {
                throw new ParseException("Incorrect module specified");
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
    }

}
