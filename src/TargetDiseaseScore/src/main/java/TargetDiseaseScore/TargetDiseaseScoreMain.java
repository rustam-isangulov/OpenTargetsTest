package TargetDiseaseScore;

import TargetDiseaseScore.cli.CommandLineParameters;
import TargetDiseaseScore.dto.*;
import TargetDiseaseScore.io.JsonIO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static TargetDiseaseScore.Calculator.median;
import static TargetDiseaseScore.io.JsonIO.exportToJson;

public class TargetDiseaseScoreMain {
    public static void main(String... args) {
        //
        // understand user defined options
        //

        CommandLineParameters.parse(args);

        System.out.println();
        System.out.println("Proceeding with following parameters");
        System.out.println("\tEvidence path: [" + CommandLineParameters.getPathToEvidence() + "]");
        System.out.println("\tTargets path: [" + CommandLineParameters.getPathToTargets() + "]");
        System.out.println("\tDiseases path: [" + CommandLineParameters.getPathToDiseases() + "]");
        System.out.println("\tOutput path: [" + CommandLineParameters.getPathToOutput() + "]");
        System.out.println("\tMin number of shared connections: [" + CommandLineParameters.getMinSharedNumber() + "]");

        //
        // run the job
        //

        //
        // test part 1
        //

        System.out.println();
        System.out.println("Start processing evidence data...");
        long startTime = System.nanoTime();

        // Generate overall association scores
        List<TDComposite> overallList = ProcessTDEvidence
                (CommandLineParameters.getPathToEvidence());

        long elapsedTime = System.nanoTime() - startTime;
        System.out.println();
        System.out.format("Total elapsed time for the evidence processing: %.0f (ms)", elapsedTime * 1e-6);
        System.out.println();
        System.out.println("Number of target-disease overall scores: " + overallList.size());

        System.out.println();
        System.out.println("Start processing target data...");
        startTime = System.nanoTime();

        // Process targets dataset
        Map<String, Target> targetMap = ProcessTargets
                (CommandLineParameters.getPathToTargets());

        elapsedTime = System.nanoTime() - startTime;
        System.out.println();
        System.out.format("Total elapsed time for the target processing: %.0f (ms)", elapsedTime * 1e-6);
        System.out.println();
        System.out.println("Number of targets: " + targetMap.size());

        System.out.println();
        System.out.println("Start processing disease data...");
        startTime = System.nanoTime();

        // Process diseases dataset
        Map<String, Disease> diseaseMap = ProcessDiseases
                (CommandLineParameters.getPathToDiseases());

        elapsedTime = System.nanoTime() - startTime;
        System.out.println();
        System.out.format("Total elapsed time for the diseases processing: %.0f (ms)", elapsedTime * 1e-6);
        System.out.println();
        System.out.println("Number of diseases: " + diseaseMap.size());


        System.out.println();
        System.out.println("Create and export joint Association/Target/Disease dataset...");

        // make a joint query with three tables
        // and save result in a *.json file
        exportToJson(jointQuery(overallList, targetMap, diseaseMap)
                , CommandLineParameters.getPathToOutput()
                , "joint_dataset.json");

        System.out.println("Json export is done");

        //
        // test part 2
        //

        System.out.println();
        System.out.println("Start searching for targets with shared disease connections...");
        startTime = System.nanoTime();

        // search for target pairs that share a min number of disease connections
        List<TargetOverlapPair> targetOverlapPairList
                = getTargetPairsWithSharedDiseases(overallList, CommandLineParameters.getMinSharedNumber());

        elapsedTime = System.nanoTime() - startTime;

        // report time spent to search
        System.out.println();
        System.out.format("Time to run search for target-target overlaps: %.2f (ms)", elapsedTime * 1e-6);
        System.out.println();
        System.out.printf("Number of target-target pairs with at least %d shared connections: %d"
                , CommandLineParameters.getMinSharedNumber()
                , targetOverlapPairList.size());
        System.out.println();

        System.out.println();
    }

    static List<TargetOverlapPair> getTargetPairsWithSharedDiseases
            (List<TDComposite> inTDCompositeAssociation,  int minOfSharedDiseases) {

        //
        // prep work
        //

        // group associations by TargetId
        var mapTarget = inTDCompositeAssociation.stream()
                .collect(Collectors.groupingByConcurrent(TDComposite::getTargetId));

        // generate a list of [Target] - [Disease Set] pairs ordered by number of diseases in the set
        List<TargetDiseaseSet> targetDiseaseSetList = mapTarget.entrySet().stream()
                // do not need to consider targets with fewer connections than min shared number
                .filter(e -> e.getValue().size() >= minOfSharedDiseases)
                .sorted((o1, o2) -> o1.getValue().size() - o2.getValue().size() )
                .map(e -> new TargetDiseaseSet(e.getKey(), e.getValue().stream()
                        .map(TDComposite::getDiseaseId)
                        .collect(Collectors.toUnmodifiableSet())))
                .collect(Collectors.toList());


        // generate a list of Search Cells for each Target
        // list TargetsToCheck includes only TargetDiseaseSets that come afterwards the current target
        // in the ordered targetDiseaseSetList to avoid duplication of target-target pairs
        List<TargetDiseaseSearchCell> searchCellList = IntStream.rangeClosed(0, targetDiseaseSetList.size() - 2)
                .mapToObj(i -> new TargetDiseaseSearchCell(
                        targetDiseaseSetList.get(i).getTargetId()
                        , targetDiseaseSetList.get(i).getDiseases()
                        , targetDiseaseSetList.subList(i+1, targetDiseaseSetList.size())))
                .collect(Collectors.toList());

        //
        // search
        //

        // search for overlap in each search cell
        List<TargetOverlapPair> targetOverlapPairList = searchCellList.parallelStream()
                .map(b -> b.getTargetsToCheck().stream()
                        .filter(t -> b.getDiseases().stream()
                                .filter(d -> t.getDiseases().contains(d))
                                .count() >= minOfSharedDiseases)
                        .map(t -> {
                            var intersection = new HashSet<String>(t.getDiseases());
                            intersection.retainAll(b.getDiseases());
                            return new TargetOverlapPair(b.getTargetId(), t.getTargetId(), intersection);
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return targetOverlapPairList;
    }

    static List<TDAssociation> jointQuery
            (List<TDComposite> overallList, Map<String
            , Target> targetMap, Map<String, Disease> diseaseMap) {

        // joint query for three tables
        List<TDAssociation> listOfAssociations = overallList.stream()
                //.limit(10000)
                .map(c -> new TDAssociation(
                        c.getTargetId()
                        , c.getDiseaseId()
                        , c.getMedianScore()
                        , c.getTopScores()
                        , targetMap.get(c.getTargetId()).getApprovedSymbol()
                        , diseaseMap.get(c.getDiseaseId()).getName()
                ))
                .sorted(Comparator.comparingDouble(TDAssociation::getMedian))
                .collect(Collectors.toList());

        return listOfAssociations;
    }


    static Map<String, Disease> ProcessDiseases(Path diseasesDir) {
        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        try(var s = Files.list(diseasesDir)
                .filter(p -> jsonMatcher.matches(p.getFileName()))) {

            // collect diseases into a [DiseaseId] - [Disease] map
            Map<String, Disease> diseases = s
                    .flatMap(f -> JsonIO.ParseFile(f, Disease.class).stream())
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Disease::getId, Function.identity()));

            return diseases;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing disease files: something bad happened with IO...", ex);
        }
    }

    static Map<String, Target> ProcessTargets(Path targetDir) {
        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        try(var s = Files.list(targetDir)
                .filter(p -> jsonMatcher.matches(p.getFileName()))) {

            // Collect all targets into an [TargetId] - [Target] map
            Map<String, Target> targets = s
                    .flatMap(f -> JsonIO.ParseFile(f, Target.class).stream())
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Target::getId, Function.identity()));

            return targets;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing target files: something bad happened with IO...", ex);
        }
    }

    static List<TDComposite> ProcessTDEvidence(Path evidenceDir) {
        // number of top scores
        final int numberOfTopScores = 3;

        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        // load and process each *.json file in the evidenceDir
        try (var s = Files.list(evidenceDir)
                .filter(p -> jsonMatcher.matches(p.getFileName()))) {

            // parse data file and individual evidence
            // and collect them into [Target:Disease] - [list of evidence] map
            var map = s
                    .flatMap(f -> JsonIO.ParseFile(f, TDEvidence.class).stream())
                    .parallel()
                    .collect(Collectors.groupingByConcurrent
                            (e -> e.getTargetId() + e.getDiseaseId()));

            // process the map into a list of overall associations
            // that includes median of scores and top 3 scores
            var composites =  map.values()
                    .parallelStream()
                    .map(e -> new TDComposite(
                            e.get(0).getTargetId(),
                            e.get(0).getDiseaseId(),
                            median(e.stream()
                                    .mapToDouble(TDEvidence::getScore)
                                    .toArray()),
                            e.stream()
                                    .map(TDEvidence::getScore)
                                    .sorted(Comparator.reverseOrder())
                                    .limit(numberOfTopScores)
                                    .collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());

            return composites;

        } catch (IOException ex) {
            throw new RuntimeException("Generating target-disease overall score : something bad happened with IO...", ex);
        }
    }
}
















