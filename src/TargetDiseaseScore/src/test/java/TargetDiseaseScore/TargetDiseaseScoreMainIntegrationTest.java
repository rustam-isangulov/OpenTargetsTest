package TargetDiseaseScore;

import TargetDiseaseScore.dto.*;
import TargetDiseaseScore.io.JsonIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetDiseaseScoreMainIntegrationTest {

    private final TargetDiseaseScoreMain processor = new TargetDiseaseScoreMain();
    private final JsonIO jsonIO = new JsonIO();

    @TempDir
    static Path dataDir;

    Path evidenceFile;
    Path targetFile;
    Path diseaseFile;

    @BeforeEach
    public void setUp() throws IOException {
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

        var targetList = Stream.of(t1, t2)
                .collect(Collectors.toList());

        try (var writer = Files.newBufferedWriter(targetFile)) {
            jsonIO.ObjToJson(targetList, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }


        var d1 = new Disease("1", "Disease One");
        var d2 = new Disease("2", "Disease Two");
        var d3 = new Disease("3", "Disease Three");

        var diseaseList = Stream.of(d1, d2, d3)
                .collect(Collectors.toList());

        try (var writer = Files.newBufferedWriter(diseaseFile)) {
            jsonIO.ObjToJson(diseaseList, writer);
        } catch (IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...", ex);
        }
    }

    @Nested
    @DisplayName("Given prepared data file structure")
    public class testOnFiles {

        @Nested
        @DisplayName("When we run the main method with a correct argument string")
        public class testMainWithCorrectArgs {
            @Test
            @DisplayName("Then the method processes data correctly")
            public void givenDataFiles_runMain() throws IOException {

                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));


                TargetDiseaseScoreMain.main("-e", evidenceFile.getParent().toString()
                        , "-t", targetFile.getParent().toString()
                        , "-d", diseaseFile.getParent().toString()
                        , "-o", dataDir.toString()
                        , "-sn", "2");


                // restore standard output
                System.setOut(standardOut);


                // assert content of the output
                assertAll("Test that the output has the correct information"
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("Number of targets: 2")
                                        , () -> "The output should have 'Number of targets: 2'")
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("Number of diseases: 3")
                                        , () -> "The output should have 'Number of diseases: 3'")
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("association scores: 5")
                                        , () -> "The output should have 'association scores: 5'")
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("shared connections: 1")
                                        , () -> "The output should have 'shared connections: 1'")
                );
            }
        }

        @Nested
        @DisplayName("When we run the main method with an incorrect argument string")
        public class testMainWithBadArgs {
            @Test
            @DisplayName("Then the method fails at the argument parsing stage")
            public void givenDataFiles_runMain() throws IOException {

                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));


                TargetDiseaseScoreMain.main("-e", evidenceFile.getParent().toString()
                        , "-badargument", targetFile.getParent().toString()
                        , "-d", diseaseFile.getParent().toString()
                        , "-o", dataDir.toString()
                        , "-sn", "2");


                // restore standard output
                System.setOut(standardOut);


                // assert content of the output
                assertAll("Test that the output has error message and help printout"
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("Parsing of command line arguments failed:")
                                        , () -> "The output should have 'Parsing of command line arguments failed:'")
                        , () -> assertTrue
                                (outputStreamCaptor.toString().contains("usage: java -jar overallscore.jar")
                                        , () -> "The output should have 'usage: java -jar overallscore.jar'")
                );
            }
        }
    }
}




















