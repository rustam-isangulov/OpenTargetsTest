package TargetDiseaseScore.cli;

import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Path;

public class CommandLineParameters {

    private int numberOfTopScores = 3;
    private int minNumberOfSharedDiseases = 2;
    private Path pathToTargets;
    private Path pathToDiseases;
    private Path pathToEvidence;
    private Path pathToOutput;

    private final Option targetsDir = Option.builder()
            .option("t")
            .longOpt("targets")
            .argName("targets_dir")
            .required()
            .hasArg()
            .desc("directory that contains targets *.json files")
            .build();

    private final Option diseasesDir = Option.builder()
            .option("d")
            .longOpt("diseases")
            .argName("diseases_dir")
            .required()
            .hasArg()
            .desc("directory that contains diseases *.json files")
            .build();

    private final Option evidenceDir = Option.builder()
            .option("e")
            .longOpt("evidence")
            .argName("evidence_dir")
            .required()
            .hasArg()
            .desc("directory that contains evidence *.json files")
            .build();

    private final Option outputDir = Option.builder()
            .option("o")
            .longOpt("output")
            .argName("output_dir")
            .required()
            .hasArg()
            .desc("directory for the overall association scores output *.json file")
            .build();


    private final Option sharedNum = Option.builder()
            .option("sn")
            .longOpt("sharednumber")
            .argName("number")
            .type(Integer.class)
            .hasArg()
            .desc("min number of shared diseases for target-target shared connection statistics")
            .build();


    private final Option topScores = Option.builder()
            .option("ts")
            .longOpt("topscores")
            .argName("number_top_scores")
            .type(Integer.class)
            .hasArg()
            .desc("number of top scores for overall statistics")
            .build();

    private final Options options = new Options();

    {
        options.addOption(evidenceDir);
        options.addOption(targetsDir);
        options.addOption(diseasesDir);
        options.addOption(outputDir);
        options.addOption(sharedNum);
        options.addOption(topScores);
    }

    public int getNumberOfTopScores() {
        return numberOfTopScores;
    }
    public int getMinSharedNumber() {
        return minNumberOfSharedDiseases;
    }

    public Path getPathToTargets() {
        return pathToTargets;
    }

    public Path getPathToDiseases() {
        return pathToDiseases;
    }

    public Path getPathToEvidence() {
        return pathToEvidence;
    }

    public Path getPathToOutput() {
        return pathToOutput;
    }

    public void parse(String... args) throws ParseException {

        // parse the command line
        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        // parse shared number of diseases option
        try {
            numberOfTopScores = Integer
                    .parseInt(line.getOptionValue(topScores, "3"));
        } catch (NumberFormatException ex) {
            throw new ParseException("Bad value for "
                    + "<" + topScores.getArgName() + ">");
        }

        // parse shared number of diseases option
        try {
            minNumberOfSharedDiseases = Integer
                    .parseInt(line.getOptionValue(sharedNum, "2"));
        } catch (NumberFormatException ex) {
            throw new ParseException("Bad value for "
                    + "<" + sharedNum.getArgName() + ">");
        }

        // parse targets path
        pathToTargets = Path.of(line.getOptionValue(targetsDir));

        if (!Files.exists(pathToTargets) && !Files.isDirectory(pathToTargets)) {
            throw new ParseException("Bad value for "
                    + "<" + targetsDir.getArgName() + ">");
        }

        // parse diseases path
        pathToDiseases = Path.of(line.getOptionValue(diseasesDir));

        if (!Files.exists(pathToDiseases) && !Files.isDirectory(pathToDiseases)) {
            throw new ParseException("Bad value for "
                    + "<" + diseasesDir.getArgName() + ">");
        }

        // parse evidence path
        pathToEvidence = Path.of(line.getOptionValue(evidenceDir));

        if (!Files.exists(pathToEvidence) && !Files.isDirectory(pathToEvidence)) {
            throw new ParseException("Bad value for "
                    + "<" + evidenceDir.getArgName() + ">");
        }

        // parse output path
        pathToOutput = Path.of(line.getOptionValue(outputDir));

        if (!Files.exists(pathToOutput) && !Files.isDirectory(pathToOutput)) {
            throw new ParseException("Bad value for "
                    + "<" + outputDir.getArgName() + ">");
        }
   }

   public void printReport() {
       System.out.println("\tEvidence path: [" + this.getPathToEvidence() + "]");
       System.out.println("\tTargets path: [" + this.getPathToTargets() + "]");
       System.out.println("\tDiseases path: [" + this.getPathToDiseases() + "]");
       System.out.println("\tOutput path: [" + this.getPathToOutput() + "]");
       System.out.println("\tMin number of shared connections: [" + this.getMinSharedNumber() + "]");
       System.out.println("\tNumber of top scores: [" + this.getNumberOfTopScores() + "]");
   }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        String footer = "\nExample:\n java -jar overallscore.jar -e \"./evidence/sourceId=eva/\" -t \"./targets/\" -d \"./diseases/\" -o \"./output/\" -sn 2";
        formatter.printHelp
                ("java -jar overallscore.jar"
                        , "\nGenerate the overall association scores for given target-disease associations"
                        + " and Calculate the number of target-target pairs that share a connection to a specified number of diseases."
                        + "\n\nOptions:" , options, footer, true);
    }
}
