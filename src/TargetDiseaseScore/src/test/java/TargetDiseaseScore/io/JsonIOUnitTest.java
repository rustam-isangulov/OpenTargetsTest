package TargetDiseaseScore.io;

import TargetDiseaseScore.dto.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JsonIOUnitTest {

    List<Target> targetList;

    @TempDir
    static Path dataDir;

    Path targetFile;

    @BeforeEach
    public void setUp() {

        var t1 = new Target("1", "AAA");
        var t2 = new Target("2", "BBB");

        targetList = Stream.of(t1, t2)
                .collect(Collectors.toList());

        targetFile = dataDir.resolve(Path.of("t.json"));
    }

    @Test
    @DisplayName("Test JsonIO writing")
    public void testWriting() {
        JsonIO jsonIO = new JsonIO();

        try (var writer = Files.newBufferedWriter(targetFile)) {
            jsonIO.ObjToJson(targetList, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (var strings = Files.lines(targetFile)) {
            var stringList = strings.collect(Collectors.toList());

            assertAll("Test that created files contains two target json strings"
                    , () -> assertEquals(2, stringList.size())
                    , () -> assertTrue(stringList.contains("{\"id\":\"1\",\"approvedSymbol\":\"AAA\"}"))
                    , () -> assertTrue(stringList.contains("{\"id\":\"2\",\"approvedSymbol\":\"BBB\"}"))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
















