package TargetDiseaseScore;

import TargetDiseaseScore.cli.ProgressReporter;
import TargetDiseaseScore.dto.Disease;
import TargetDiseaseScore.dto.TDEvidence;
import TargetDiseaseScore.dto.Target;
import TargetDiseaseScore.io.JsonIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TargetDiseaseScoreMainUnitTest {
    private final TargetDiseaseScoreMain processor = new TargetDiseaseScoreMain();
    private final JsonIO jsonIO = new JsonIO();

    @TempDir
    static Path dataDir;

    Path evidenceFile;
    Path targetFile;
    Path diseaseFile;

    Predicate<Path> jsonFilter;

    @BeforeEach
    public void setUp() throws IOException {

        // setup json files filter

        final PathMatcher jsonMatch = FileSystems.getDefault()
                .getPathMatcher("glob:*.json");
        jsonFilter = path ->
                jsonMatch.matches(path.getFileName());

        // prepare source data file structure

        evidenceFile = dataDir.resolve(Path.of("evidence/e.json"));
        Files.createDirectories(evidenceFile.getParent());

        targetFile = dataDir.resolve("targets/t.json");
        Files.createDirectories(targetFile.getParent());

        diseaseFile = dataDir.resolve("diseases/d.json");
        Files.createDirectories(diseaseFile.getParent());

        // populate data files

        var e1 = new TDEvidence("1", "1", 0.0);
        var e2 = new TDEvidence("1", "1", 1.0);
        var e3 = new TDEvidence("1", "1", 2.0);

        var e4 = new TDEvidence("1", "2", 0.0);
        var e5 = new TDEvidence("1", "2", 0.0);
        var e6 = new TDEvidence("1", "2", 1.0);
        var e7 = new TDEvidence("1", "2", 2.0);

        var e8 = new TDEvidence("2", "1", 0.0);
        var e9 = new TDEvidence("2", "1", 3.0);
        var e10 = new TDEvidence("2", "1", 5.0);

        var e11 = new TDEvidence("2", "2", 2.0);
        var e12 = new TDEvidence("2", "2", 4.0);
        var e13 = new TDEvidence("2", "2", 6.0);

        var e14 = new TDEvidence("2", "3", 0.2);
        var e15 = new TDEvidence("2", "3", 0.4);
        var e16 = new TDEvidence("2", "3", 0.6);

        var evidenceList = Stream.of
                        (e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16)
                .collect(Collectors.toList());

        try (var writer = Files.newBufferedWriter(evidenceFile)) {
            jsonIO.ObjToJson(evidenceList, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }


        var t1 = new Target("1", "AAA");
        var t2 = new Target("2", "BBB");

        var targetList = Stream.of(t1, t2).collect(Collectors.toList());

        try (var writer = Files.newBufferedWriter(targetFile)) {
            jsonIO.ObjToJson(targetList, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }


        var d1 = new Disease("1", "Disease One");
        var d2 = new Disease("2", "Disease Two");
        var d3 = new Disease("3", "Disease Three");

        var diseaseList = Stream.of(d1, d2, d3).collect(Collectors.toList());

        try (var writer = Files.newBufferedWriter(diseaseFile)) {
            jsonIO.ObjToJson(diseaseList, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }
    }

    @Test
    @DisplayName("Test map and map_of_objects extraction from json files")
    public void testMapExtraction(@TempDir Path dataDir) {

        // extract maps from the source files

        var evidenceMap = processor.getMapOfGroups
                (evidenceFile.getParent(), jsonFilter
                        , TDEvidence.class, e -> e.getTargetId() + e.getDiseaseId()
                        , jsonIO );

        var targetMap = processor.getMapOfObjects
                (targetFile.getParent(), jsonFilter
                        , Target.class, Target::getId, jsonIO);

        var diseaseMap = processor.getMapOfObjects
                (diseaseFile.getParent(), jsonFilter
                        , Disease.class, Disease::getId, jsonIO);


        assertAll("Test that we loaded the correct number of objects"
                , () -> assertEquals(5, evidenceMap.size())
                , () -> assertEquals(2, targetMap.size())
                , () -> assertEquals(3, diseaseMap.size()));
    }

    @Test
    @DisplayName("Test generating Overall Scores")
    public void testOverallScores() {

        // extract [target-disease]-[evidence list] map
        var evidenceMap = processor.getMapOfGroups
                (evidenceFile.getParent(), jsonFilter
                        , TDEvidence.class, e -> e.getTargetId() + e.getDiseaseId()
                        , jsonIO);

        // finally generate overall scores
        var overallScores = processor
                .generateOverallScores(evidenceMap, 3);


        // expected list:
        // targetId = 2, diseaseId = 1, medianScore = 3.00, topScores=[5.0, 3.0, 0.0]
        // targetId = 2, diseaseId = 2, medianScore = 4.00, topScores=[6.0, 4.0, 2.0]
        // targetId = 1, diseaseId = 1, medianScore = 1.00, topScores=[2.0, 1.0, 0.0]
        // targetId = 1, diseaseId = 2, medianScore = 0.50, topScores=[2.0, 1.0, 0.0]
        // targetId = 2, diseaseId = 3, medianScore = 0.40, topScores=[0.6, 0.4, 0.2]

        assertAll("Test generating overall scores for all target-disease pairs"
                , () -> assertEquals(5, overallScores.size())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getMedianScore() == 0.4).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getMedianScore() == 0.5).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getMedianScore() == 1).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getMedianScore() == 3.0).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getMedianScore() == 4.0).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getTopScores().equals(List.of(0.6, 0.4, 0.2))).count())
                , () -> assertEquals(2, overallScores.stream()
                        .filter(e -> e.getTopScores().equals(List.of(2.0, 1.0, 0.0))).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getTopScores().equals(List.of(6.0, 4.0, 2.0))).count())
                , () -> assertEquals(1, overallScores.stream()
                        .filter(e -> e.getTopScores().equals(List.of(5.0, 3.0, 0.0))).count())
        );
    }

    @Test
    @DisplayName("Test generating joint Association/Target/Disease data set")
    public void testJointQuery() {
        // extract maps from the source files

        var evidenceMap = processor.getMapOfGroups
                (evidenceFile.getParent(), jsonFilter
                        , TDEvidence.class, e -> e.getTargetId() + e.getDiseaseId()
                        , jsonIO );

        var targetMap = processor.getMapOfObjects
                (targetFile.getParent(), jsonFilter
                        , Target.class, Target::getId, jsonIO);

        var diseaseMap = processor.getMapOfObjects
                (diseaseFile.getParent(), jsonFilter
                        , Disease.class, Disease::getId, jsonIO);

        // get overall score
        var overallScores = processor
                .generateOverallScores(evidenceMap, 3);

        // finally run the joint query
        var jointData = processor
                .jointQuery(overallScores, targetMap, diseaseMap);

        // expected list:
        // targetId = 2, diseaseId = 3, median = 0.40, top3 = [0.6, 0.4, 0.2], approvedSymbol = BBB, name = [Disease Three]
        // targetId = 1, diseaseId = 2, median = 0.50, top3 = [2.0, 1.0, 0.0], approvedSymbol = AAA, name = [Disease Two]
        // targetId = 1, diseaseId = 1, median = 1.00, top3 = [2.0, 1.0, 0.0], approvedSymbol = AAA, name = [Disease One]
        // targetId = 2, diseaseId = 1, median = 3.00, top3 = [5.0, 3.0, 0.0], approvedSymbol = BBB, name = [Disease One]
        // targetId = 2, diseaseId = 2, median = 4.00, top3 = [6.0, 4.0, 2.0], approvedSymbol = BBB, name = [Disease Two]

        assertAll("Test generating joint data set Association/Target/Disease"
                , () -> assertEquals(5, jointData.size())
                , () -> assertEquals(2, jointData.stream()
                        .filter(e -> e.getApprovedSymbol().equals("AAA")).count())
                , () -> assertEquals(3, jointData.stream()
                        .filter(e -> e.getApprovedSymbol().equals("BBB")).count())
                , () -> assertEquals(2, jointData.stream()
                        .filter(e -> e.getName().equals("Disease One")).count())
                , () -> assertEquals(2, jointData.stream()
                        .filter(e -> e.getName().equals("Disease Two")).count())
                , () -> assertEquals(1, jointData.stream()
                        .filter(e -> e.getName().equals("Disease Three")).count())
                , () -> assertEquals(2, jointData.stream()
                        .filter(e -> e.getTop3().equals(List.of(2.0, 1.0, 0.0))).count())
        );
    }

    @Test
    @DisplayName("Test searching for target-target pairs that share at least 2 diseases")
    public void testTargetTargetSearch() {
        // extract maps from the source files

        var evidenceMap = processor.getMapOfGroups
                (evidenceFile.getParent(), jsonFilter
                        , TDEvidence.class, e -> e.getTargetId() + e.getDiseaseId()
                        , jsonIO );

        var targetMap = processor.getMapOfObjects
                (targetFile.getParent(), jsonFilter
                        , Target.class, Target::getId, jsonIO);

        var diseaseMap = processor.getMapOfObjects
                (diseaseFile.getParent(), jsonFilter
                        , Disease.class, Disease::getId, jsonIO);

        // get overall score
        var overallScores = processor
                .generateOverallScores(evidenceMap, 3);

        // finally perform the search
        var targetOverlaps = processor
                .getTargetPairsWithSharedDiseases(overallScores, 2);

        // expected list:
        // TargetA:[1] TargetB:[2] Shared diseases:[1, 2]

        assertAll("Test searching for target-target pairs"
                , () -> assertEquals(1, targetOverlaps.size())
                , () -> assertEquals(1, targetOverlaps.stream()
                        .filter(e -> e.getTargetIdA().equals("1")).count())
                , () -> assertEquals(1, targetOverlaps.stream()
                        .filter(e -> e.getTargetIdB().equals("2")).count())
                , () -> assertEquals(1, targetOverlaps.stream()
                        .filter(e -> e.getDiseasesShared().equals(Set.of("1", "2"))).count())
        );
    }
}


























