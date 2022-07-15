package TargetDiseaseScore;

import TargetDiseaseScore.cli.CommandLineParameters;
import TargetDiseaseScore.dto.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TargetDiseaseScoreMain {

    static long totalFileProcessing = 0;

    public static void main(String... args) {
        System.out.println("TargetDiseaseScoreMain is starting...");
        System.out.println();

        //
        // understand user defined options
        //

        CommandLineParameters.parse(args);

        System.out.println("Proceeding with following paramaters");
        System.out.println("\tEvidence path: [" + CommandLineParameters.getPathToEvidence() + "]");
        System.out.println("\tTargets path: [" + CommandLineParameters.getPathToTargets() + "]");
        System.out.println("\tDiseases path: [" + CommandLineParameters.getPathToDiseases() + "]");
        System.out.println("\tOutput path: [" + CommandLineParameters.getPathToOutput() + "]");
        System.out.println("\tMin number of shared connections: [" + CommandLineParameters.getMinSharedNumber() + "]");

        //
        // run the job
        //

        // test part 1
        List<TDComposite> overallList = ProcessTDEvidence();
        Map<String, Target> targetMap = GetAllTargets();
        Map<String, Disease> diseaseMap = GetAllDiseases();

        jointQuery(overallList, targetMap, diseaseMap);

        // test part 2
        List<TargetOverlapPair> targetOverlapPairList
                = getTargetPairsWithSharedDiseases(overallList, CommandLineParameters.getMinSharedNumber());

        System.out.println();
        System.out.println("TargetDiseaseScoreMain has completed.");
    }

    static List<TargetOverlapPair> getTargetPairsWithSharedDiseases
            (List<TDComposite> inTDCompositeAssociation,  int minOfSharedDiseases) {

        //
        // prep work
        //

        // group associations by TagetId
        var mapTarget = inTDCompositeAssociation.stream()
                .collect(Collectors.groupingByConcurrent(TDComposite::getTargetId));

        System.out.println();
        System.out.println("unique target id map.size: " + mapTarget.size());

        // generate a list of [Target] - [Disease Set] pairs ordered by number of diseases in the set
        List<TargetDiseaseSet> targetDiseaseSetList = mapTarget.entrySet().stream()
                .filter(e -> e.getValue().size() >= minOfSharedDiseases)
                .sorted((o1, o2) -> o1.getValue().size() - o2.getValue().size() )
                .map(e -> new TargetDiseaseSet(e.getKey(), e.getValue().stream()
                        .map(TDComposite::getDiseaseId)
                        .collect(Collectors.toUnmodifiableSet())))
                .collect(Collectors.toList());


        // generate a list of Search Cells for each Target
        // list TargetsToCheck includes only TargetDiseaseSets that come afterwards
        // in the ordered targetDiseaseSetList to avoid duplication of target-target pairs
        List<TargetDiseaseSearchCell> searchCellList = IntStream.rangeClosed(0, targetDiseaseSetList.size() - 2)
                .mapToObj(i -> new TargetDiseaseSearchCell(
                        targetDiseaseSetList.get(i).getTargetId()
                        , targetDiseaseSetList.get(i).getDiseases()
                        , targetDiseaseSetList.subList(i+1, targetDiseaseSetList.size())))
                .collect(Collectors.toList());

        System.out.println("Number of search cells: " + searchCellList.size());
        System.out.println();

        //
        // search
        //

        long startTime = System.nanoTime();

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

        System.out.println("Number of overlapping target-target pairs: " + targetOverlapPairList.size());

        long elapsedTime = System.nanoTime() - startTime;

        // report time spent to search
        System.out.format("Time to run search for target-target overlaps: %.2f (ms)", elapsedTime * 1e-6);
        System.out.println();


        return targetOverlapPairList;
    }

    static void jointQuery(List<TDComposite> overallList, Map<String, Target> targetMap, Map<String, Disease> diseaseMap) {
        // joint query for three tables
        List<TDAssociation> listToOutput = overallList.stream()
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

        exportToJson(listToOutput);
    }

    static void exportToJson(List<TDAssociation> list)  {
        Path targetDir = Path.of("../../output/associations");

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);

        Path file = targetDir.resolve("test_one.json");

        // prepare to measure elapsed time
        long startTime = System.nanoTime();

        try (var writer = Files.newBufferedWriter(file)) {
            list.forEach(a -> {
                        try {
                            writer.write(objectMapper.writeValueAsString(a));
                            writer.newLine();
                        } catch (IOException ex) {
                            throw new RuntimeException("JSON write has failed...", ex);
                        }
                    });

            // report elapsed time
            long elapsedTime = System.nanoTime() - startTime;

            // report time spent to read and parse files
            System.out.format("Time to write json files: %.2f (ms)", elapsedTime * 1e-6);
            System.out.println();

            System.out.println("Json write is done");
        } catch(IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...");
        }
    }

    static Map<String, Disease> GetAllDiseases() {
        Path targetDir = Path.of("../../data/diseases");

        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        try(var s = Files.list(targetDir)) {
            // prepare to measure elapsed time
            long startTime = System.nanoTime();

            // create a list of composite evidence objects with median and top 3 scores
            Map<String, Disease> diseases = s
                    .filter(p -> jsonMatcher.matches(p.getFileName()))
                    .flatMap(f -> ParseDiseaseFile(f).stream())
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Disease::getId, Function.identity()));

            System.out.println();
            System.out.println("Disease list size: " + diseases.size());

            // report elapsed time
            long elapsedTime = System.nanoTime() - startTime;

            // report time spent to read and parse files
            System.out.format("Time to parse files: %.2f (ms)", elapsedTime * 1e-6);
            System.out.println();

            return diseases;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing target files: something bad happened with IO...", ex);
        }
    }

    static List<Disease> ParseDiseaseFile(Path inFile) {
        System.out.print("Processing: [" + inFile.getFileName() + "]");


        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try( var s = Files.lines(inFile)) {


            var list = s.parallel()
                    .map(l -> {
                        try{
                            return mapper.readValue(l, Disease.class);
                        } catch (IOException ex) {
                            throw new RuntimeException("JSON mapping failed...", ex);
                        }
                    })
                    .collect(Collectors.toList());

            System.out.print(" lines: [" + list.size() + "]");
            System.out.println();

            return list;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing evidence files: something bad happened with IO...", ex);
        }
    }


    static Map<String, Target> GetAllTargets() {
        Path targetDir = Path.of("../../data/targets");

        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        try(var s = Files.list(targetDir)) {
            // prepare to measure elapsed time
            long startTime = System.nanoTime();

            // create a list of composite evidence objects with median and top 3 scores
            Map<String, Target> targets = s
                    .filter(p -> jsonMatcher.matches(p.getFileName()))
                    .flatMap(f -> ParseTargetFile(f).stream())
                    .parallel()
                    .collect(Collectors.toConcurrentMap(Target::getId, Function.identity()));

            System.out.println();
            System.out.println("Target list size: " + targets.size());

            // report elapsed time
            long elapsedTime = System.nanoTime() - startTime;

            // report time spent to read and parse files
            System.out.format("Time to parse files: %.0f (ms)", elapsedTime * 1e-6);
            System.out.println();

            return targets;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing target files: something bad happened with IO...", ex);
        }
    }

    static List<Target> ParseTargetFile(Path inFile) {
        System.out.print("Processing: [" + inFile.getFileName() + "]");


        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try( var s = Files.lines(inFile)) {


            var list = s.parallel()
                    .map(l -> {
                        try{
                            return mapper.readValue(l, Target.class);
                        } catch (IOException ex) {
                            throw new RuntimeException("JSON mapping failed...", ex);
                        }
                    })
                    .collect(Collectors.toList());

            System.out.print(" lines: [" + list.size() + "]");
            System.out.println();

            return list;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing evidence files: something bad happened with IO...", ex);
        }
    }

    static double median(double[] l) {
        if (l == null)
            throw new RuntimeException("Attempt to calculate median value on a null list");
        if (l.length == 0)
            throw new RuntimeException("Attempt to calculate median value on an empty list");

        // sort first
        Arrays.sort(l);

        if (l.length % 2 == 0) {
            // this is to avoid double precision errors for simple score computation
            return BigDecimal.valueOf(l[l.length/2])
                    .add(BigDecimal.valueOf(l[l.length/2 - 1]))
                    .divide(BigDecimal.valueOf(2.0)).doubleValue();
        } else {
            return l[l.length/2];
        }
    }

    static List<TDComposite> ProcessTDEvidence() {
        Path evidenceDir = Path.of("../../data/evidence/sourceId=eva");

        // filter for json files
        PathMatcher jsonMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");

        try {
            System.out.format("Total files to process: %d", Files.list(evidenceDir).count());
            System.out.println();

            // prepare to measure elapsed time
            long startTime = System.nanoTime();

            // create a list of composite evidence objects with median and top 3 scores
            List<TDComposite> composites = Files.list(evidenceDir)
                    .filter(p -> jsonMatcher.matches(p.getFileName()))
                    .flatMap(f -> ParseTDEvidenceFile(f).stream())
                    .parallel()
                    .collect(Collectors.groupingByConcurrent
                            (e -> e.getTargetId() + e.getDiseaseId()))
                    .values()
                    .stream()
                    .map(e -> new TDComposite(
                            e.get(0).getTargetId(),
                            e.get(0).getDiseaseId(),
                            median(e.stream()
                                    .mapToDouble(TDEvidence::getScore)
                                    .toArray()),
                            e.stream()
                                    .map(TDEvidence::getScore)
                                    .sorted(Comparator.reverseOrder())
                                    .limit(3)
                                    .collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());

            // report elapsed time
            long elapsedTimeComp = System.nanoTime() - startTime;

            // report time spent to read and parse files
            System.out.println();
            System.out.format("Time to parse all files: %d (ms)", totalFileProcessing);
            System.out.println();
            System.out.format("Time to generate overall scores: %.2f (ms)", elapsedTimeComp * 1e-6 - totalFileProcessing);
            System.out.println();
            System.out.format("Total pipeline elapsed time: %.0f (ms)", elapsedTimeComp * 1e-6);
            System.out.println();

            System.out.println();
            System.out.println("Target-disease overall scores: " + composites.size());
            System.out.println();

            return composites;

        } catch (IOException ex) {
            throw new RuntimeException("Generating target-disease overall score : something bad happened with IO...", ex);
        }
    }

    static List<TDEvidence> ParseTDEvidenceFile(Path inFile) {
        System.out.print("Processing: [" + inFile.getFileName() + "]");

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try( var s = Files.lines(inFile)) {
            // prepare to measure elapsed time
            long startTime = System.nanoTime();

            var list = s.parallel()
                    .map(l -> {
                        try{
                            return mapper.readValue(l, TDEvidence.class);
                        } catch (IOException ex) {
                            throw new RuntimeException("JSON mapping failed...", ex);
                        }
                    })
                    .collect(Collectors.toList());

            // report elapsed time
            long elapsedTime = System.nanoTime() - startTime;

            // update totals for elapsed times
            totalFileProcessing += elapsedTime * 1e-6;

            System.out.print(" lines: [" + list.size() + "]");
            System.out.format(" time: [%.2f (ms)]" , elapsedTime * 1e-6);
            System.out.println();

            return list;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing evidence files: something bad happened with IO...", ex);
        }
    }
}
















