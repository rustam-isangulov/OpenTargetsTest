package TargetDiseaseScore.cli;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineParametersUnitTest {

    private final CommandLineParameters clp = new CommandLineParameters();

    @Test
    public void givenEmptyArgList_whenParse_reportError() {
        Throwable exception = assertThrows(ParseException.class
                , () -> clp.parse(new String[]{"-o", "./data/output"}));
    }

    @Test
    public void givenAllOptions_whenDirsDoNotExist_throwException() {
        var dataDir = Path.of("that",
                "/", "does", "/", "not", "/", "exist");

        Throwable exception = assertThrows(ParseException.class
                , () -> clp.parse("-o", dataDir.toString()
                        , "-e", dataDir.toString(), "-t", dataDir.toString()
                        , "-d", dataDir.toString()));
    }

    @Test
    public void givenAllOptions_whenDirsExist_valuesAreAvailable(@TempDir Path dataDir) {
        try {
            clp.parse("-o", dataDir.toString()
                    , "-e", dataDir.toString(), "-t", dataDir.toString()
                    , "-d", dataDir.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        assertAll("directories",
                ()-> assertEquals(clp.getPathToOutput(), dataDir),
                ()-> assertEquals(clp.getPathToDiseases(), dataDir),
                ()-> assertEquals(clp.getPathToTargets(), dataDir),
                ()-> assertEquals(clp.getPathToEvidence(), dataDir),
                ()-> assertEquals(clp.getMinSharedNumber(), 2));
    }
}
