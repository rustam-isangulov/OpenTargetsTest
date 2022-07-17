package TargetDiseaseScore;

import TargetDiseaseScore.cli.CommandLineParameters;
import TargetDiseaseScore.cli.ProgressReporter;
import TargetDiseaseScore.dto.*;
import TargetDiseaseScore.io.JsonIO;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static TargetDiseaseScore.Calculator.median;
import static TargetDiseaseScore.cli.ProgressReporter.Action.*;

public class TargetDiseaseScoreMain {
    private final JsonIO jsonIO = new JsonIO();

    private final PathMatcher fileMatcher =  FileSystems.getDefault()
            .getPathMatcher("glob:*.json");

    public static void main(String... args) {
        //
        // understand user defined options
        //

        var clp = new CommandLineParameters();

        try {
            clp.parse(args);
        } catch (ParseException ex) {
            System.out.println("Parsing of command line arguments failed: "
                    + ex.getMessage());

            System.out.println();

            // just a bit annoying...
            // leave it for now...
            clp.printHelp();

            System.exit(1);
        }

        System.out.println();
        System.out.println("Proceeding with the following parameters");
        System.out.println("\tEvidence path: [" + clp.getPathToEvidence() + "]");
        System.out.println("\tTargets path: [" + clp.getPathToTargets() + "]");
        System.out.println("\tDiseases path: [" + clp.getPathToDiseases() + "]");
        System.out.println("\tOutput path: [" + clp.getPathToOutput() + "]");
        System.out.println("\tMin number of shared connections: [" + clp.getMinSharedNumber() + "]");

        // planning to read only json files for now...
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        //
        // run the job
        //

        TargetDiseaseScoreMain processor = new TargetDiseaseScoreMain();

        //
        // test part 1
        //

        ProgressReporter.of(EVIDENCE_FILE).start();

        // Process target-disease evidence files
        Map<String, List<TDEvidence>> evidenceMap =
                processor.processTDEvidence(clp.getPathToEvidence());

        ProgressReporter.of(EVIDENCE_FILE).finish(evidenceMap.size());


        ProgressReporter.of(EVIDENCE_MAP).start();

        // Generate overall association scores
        List<TDComposite> overallList =
                processor.generateOverallScores(evidenceMap);

        ProgressReporter.of(EVIDENCE_MAP).finish(overallList.size());


        ProgressReporter.of(TARGETS).start();

        // Process targets dataset
        Map<String, Target> targetMap =
                processor.processTargets(clp.getPathToTargets());

        ProgressReporter.of(TARGETS).finish(targetMap.size());


        ProgressReporter.of(DISEASES).start();

        // Process diseases dataset
        Map<String, Disease> diseaseMap =
                processor.processDiseases(clp.getPathToDiseases());

        ProgressReporter.of(DISEASES).finish(diseaseMap.size());


        ProgressReporter.of(JOINT_DATASET).start();

        List<TDAssociation> jointData =
                processor.jointQuery(overallList, targetMap, diseaseMap);

        processor.exportJointDataSet(jointData, clp.getPathToOutput(), "joint_dataset.json");

        ProgressReporter.of(JOINT_DATASET).finish(jointData.size());

        //
        // test part 2
        //

        ProgressReporter.of(SEARCHING).start();

        // search for target pairs that share a min number of disease connections
        List<TargetOverlapPair> targetOverlapPairList
                =  processor.getTargetPairsWithSharedDiseases(overallList, clp.getMinSharedNumber());

        ProgressReporter.of(SEARCHING).finish(targetOverlapPairList.size());

        System.out.println();
    }

    void exportJointDataSet(List<TDAssociation> inData, Path outputDir, String filePattern) {

        // for now assume single file export...
        Path file = outputDir.resolve(filePattern);

        try (var writer = Files.newBufferedWriter(file)) {
            jsonIO.ObjToJson(inData, writer);
        } catch(IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }
    }

    List<TargetOverlapPair> getTargetPairsWithSharedDiseases
            (List<TDComposite> inTDCompositeAssociation,  int minOfSharedDiseases) {

        //
        // prep work
        //

        // group associations by TargetId
        var mapTarget = inTDCompositeAssociation
                .parallelStream()
                .collect(Collectors.groupingByConcurrent(TDComposite::getTargetId));

        // generate a list of [Target] - [Disease Set] pairs ordered by number of diseases in the set
        List<TargetDiseaseSet> targetDiseaseSetList = mapTarget.entrySet()
                .parallelStream()
                // do not need to consider targets with fewer connections than min shared number
                .filter(e -> e.getValue().size() >= minOfSharedDiseases)
                .sorted((o1, o2) -> o1.getValue().size() - o2.getValue().size() )
                .map(e -> new TargetDiseaseSet(e.getKey()
                        , e.getValue()
                        .stream()
                        .map(TDComposite::getDiseaseId)
                        .collect(Collectors.toUnmodifiableSet())))
                .collect(Collectors.toList());


        // generate a list of Search Cells for each Target
        // TargetsToCheck includes only TargetDiseaseSets that
        // come afterwards the current target in the ordered targetDiseaseSetList
        // to avoid duplication of target-target pairs
        List<TargetDiseaseSearchCell> searchCellList = IntStream.rangeClosed(0, targetDiseaseSetList.size() - 2)
                .mapToObj(i -> new TargetDiseaseSearchCell(
                        targetDiseaseSetList.get(i).getTargetId()
                        , targetDiseaseSetList.get(i).getDiseases()
                        , targetDiseaseSetList.subList(i+1, targetDiseaseSetList.size())))
                .collect(Collectors.toList());

        //
        // search
        //

        // search for overlaps within each search cell
        List<TargetOverlapPair> targetOverlapPairList = searchCellList
                .parallelStream()
                .map(cell -> cell.getTargetsToCheck().stream()
                        .filter(nextSet -> cell.getDiseases().stream()
                                .filter(cellDisease -> nextSet.getDiseases().contains(cellDisease))
                                .count() >= minOfSharedDiseases)
                        .map(overlappingSet -> {
                            var intersection = new HashSet<String>(overlappingSet.getDiseases());
                            intersection.retainAll(cell.getDiseases());
                            return new TargetOverlapPair
                                    (cell.getTargetId()
                                            , overlappingSet.getTargetId()
                                            , intersection);
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return targetOverlapPairList;
    }

    List<TDAssociation> jointQuery
            (List<TDComposite> overallList
                    , Map<String, Target> targetMap
                    , Map<String, Disease> diseaseMap) {

        // joint query for three tables
        List<TDAssociation> listOfAssociations = overallList
                .stream()
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

    Map<String, Disease> processDiseases(Path diseasesDir) {

        try(var s = Files.list(diseasesDir)
                .filter(p -> fileMatcher.matches(p.getFileName()))) {

            // collect diseases into a [DiseaseId] - [Disease] map
            Map<String, Disease> diseases = s
                    .flatMap(f -> {
                        try (var lines = Files.lines(f)) {
                            return jsonIO.LinesToObj(lines, Disease.class).stream();
                        } catch (IOException ex) {
                            throw new RuntimeException("Parsing json strings: Something bad happened with IO...", ex);
                        }
                    })
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Disease::getId, Function.identity()));

            return diseases;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing disease files: something bad happened with IO...", ex);
        }
    }

    Map<String, Target> processTargets(Path targetDir) {

        try(var s = Files.list(targetDir)
                .filter(p -> fileMatcher.matches(p.getFileName()))) {

            // Collect all targets into an [TargetId] - [Target] map
            Map<String, Target> targets = s
                    .flatMap(f -> {
                        try (var lines = Files.lines(f)) {
                            return jsonIO.LinesToObj(lines, Target.class).stream();
                        } catch (IOException ex) {
                            throw new RuntimeException("Parsing json strings: Something bad happened with IO...", ex);
                        }
                    })
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Target::getId, Function.identity()));

            return targets;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing target files: something bad happened with IO...", ex);
        }
    }

    Map<String, List<TDEvidence>> processTDEvidence(Path evidenceDir) {

        // load and process each *.json file in the evidenceDir
        try (var s = Files.list(evidenceDir)
                .filter(p -> fileMatcher.matches(p.getFileName()))) {

            // parse data file and individual evidence
            // and collect them into [Target:Disease] - [list of evidence] map
            var map = s
                    .flatMap(f -> {
                        try (var lines = Files.lines(f)) {
                            return jsonIO.LinesToObj(lines, TDEvidence.class).stream();
                        } catch (IOException ex) {
                            throw new RuntimeException("Parsing json strings: Something bad happened with IO...", ex);
                        }
                    })
                    .parallel()
                    .collect(Collectors.groupingByConcurrent
                            (e -> e.getTargetId() + e.getDiseaseId()));

            return map;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing evidence files : something bad happened with IO...", ex);
        }
    }

    List<TDComposite> generateOverallScores(Map<String, List<TDEvidence>> evidenceMap) {
        // number of top scores
        final int numberOfTopScores = 3;

        // process the map into a list of overall associations
        // that includes median of scores and top 3 scores
        var composites =  evidenceMap.values()
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
    }
}








