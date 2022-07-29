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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static TargetDiseaseScore.Calculator.median;

public class TargetDiseaseScoreMain {

    public static void main(String... args) {

        var clp = new CommandLineParameters();

        try {
            // understand user defined options
            clp.parse(args);

            // ready to go
            System.out.println();
            System.out.println("Proceeding with the following parameters");
            clp.printReport();

        } catch (ParseException ex) {
            System.out.println("Parsing of command line arguments failed: "
                    + ex.getMessage());

            // just a bit annoying...
            // leave it for now...
            System.out.println();
            clp.printHelp();

            // no reasonable recovery from here...
            return;
        }


        //
        // run the job
        //

        // main processor
        TargetDiseaseScoreMain processor = new TargetDiseaseScoreMain();
        // json mapper
        JsonIO jsonIO = new JsonIO();

        // filter for *.json files
        final PathMatcher jsonMatch = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");
        // filter for json files
        Predicate<Path> jsonFilter = path ->
                jsonMatch.matches(path.getFileName());

        // progress reporter
        ProgressReporter pr = new ProgressReporter();


        //
        // test part 1
        //

        pr.mark();

        // extract evidence map and generate overall scores

        var evidenceMap = processor.getMapOfGroups
                (clp.getPathToEvidence(), jsonFilter
                        , TDEvidence.class, e -> e.getTargetId() + e.getDiseaseId()
                        , jsonIO );

        // generate overall scores
        var overallScores = processor
                .generateOverallScores(evidenceMap, clp.getNumberOfTopScores());

        pr.report("extracting evidence map and process scores"
                , "target-disease overall association scores", overallScores.size());


        // extract targets data map [TargetID] - [Target]

        Map<String, Target> targetMap = processor.getMapOfObjects
                (clp.getPathToTargets(), jsonFilter
                        , Target.class, Target::getId, jsonIO);

        pr.report("extracting targets"
                , "targets", targetMap.size());


        // extract diseases data map [DiseaseID] - [Disease]

        Map<String, Disease> diseaseMap = processor.getMapOfObjects
                (clp.getPathToDiseases(), jsonFilter
                        , Disease.class, Disease::getId, jsonIO);

        pr.report("extracting diseases"
                , "diseases", diseaseMap.size());


        // create a joint table and save as *.json file

        List<TDAssociation> jointData =
                processor.jointQuery(overallScores, targetMap, diseaseMap);

        Path outputFile = clp.getPathToOutput().resolve("joint_dataset.json");
        try (var writer = Files.newBufferedWriter(outputFile)) {
            jsonIO.ObjToJson(jointData, writer);
        } catch(IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }

        pr.report("generating joint Association/Target/Disease data set"
                , "overall association scores", jointData.size());

        //
        // test part 2
        //

        // search for target pairs that share a min number of disease connections

        List<TargetOverlapPair> targetOverlapPairList
                =  processor.getTargetPairsWithSharedDiseases(overallScores, clp.getMinSharedNumber());

        pr.report("searching for targets with shared disease connections"
                , "target-target pairs with at least "
                        + clp.getMinSharedNumber() + " shared connections", targetOverlapPairList.size());

        System.out.println();
    }

    public <T> Map<String, List<T>> getMapOfGroups
            (Path directory, Predicate<Path> jsonFilter
                    , Class<T> type, Function<T, String> groupKeyMapper
                    , JsonIO jsonIO) {

        // merger for the final map across multiple files
        BinaryOperator<List<T>> merger = (o1, o2) -> {
            o1.addAll(o2);
            return o1;
        };

        // traverse all matched files in the directory
        try( var files = Files.list(directory).filter(jsonFilter)) {

            // map each string to an object and collect
            return files
                    .parallel()
                    .flatMap(path -> {
                        try (var lines = Files.lines(path)) {
                            return lines.parallel()
                                    .map(l -> jsonIO.stringToObj(l, type))
                                    .collect(Collectors
                                            .groupingByConcurrent(groupKeyMapper))
                                    . entrySet().stream();
                        } catch (IOException ex) {
                            throw new RuntimeException
                                    ("Extracting objects from a json file: problem with IO: ", ex);
                        }
                    }).parallel()
                    .collect(Collectors
                            .toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, merger));
        } catch (IOException e) {
            throw new RuntimeException("Mapping json files to objects: something bad happened with IO: ", e);
        }
    }

    public <T> Map<String, T> getMapOfObjects
            (Path directory, Predicate<Path> jsonFilter
             , Class<T> type, Function<T, String> keyMapper, JsonIO jsonIO) {

        // traverse all matched files in the directory
        try( var files = Files.list(directory).filter(jsonFilter)) {

            // map each string to an object and collect
            return files
                    .parallel()
                    .flatMap(path -> {
                        try (var lines = Files.lines(path)) {
                            return lines.parallel()
                                    .map(l -> jsonIO.stringToObj(l, type))
                                    .collect(Collectors
                                            .toConcurrentMap(keyMapper, Function.identity()))
                                    .entrySet().stream();
                        } catch (IOException ex) {
                            throw new RuntimeException
                                    ("Extracting objects from a json file: problem with IO: ", ex);
                        }
                    }).parallel()
                    .collect(Collectors
                            .toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            throw new RuntimeException("Mapping json files to objects: something bad happened with IO: ", e);
        }
    }

    public List<TargetOverlapPair> getTargetPairsWithSharedDiseases
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
        // come after the current target in the ordered targetDiseaseSetList
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

    public List<TDAssociation> jointQuery
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

    public List<TDComposite> generateOverallScores
            (Map<String, List<TDEvidence>> evidenceMap, int numberOfTopScores) {

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








