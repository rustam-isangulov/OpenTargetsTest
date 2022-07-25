package cli;

import FtpUtil.cli.CommandLineParameters;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandLineParametersTest {

    @Nested
    @DisplayName("Given command line parameters parser has been created")
    public class testClpCreated {

        private final CommandLineParameters clp = new CommandLineParameters();

        @Nested
        @DisplayName("When we haven't parsed anything")
        public class testNoParsing {

            @Test
            @DisplayName("Then we can print help string")
            public void testHelp() {
                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));

                clp.printHelp();

                assertTrue(outputStreamCaptor.toString().startsWith("usage: java -jar ftputil.jar"));

                // restore standard output
                System.setOut(standardOut);
            }

            @Test
            @DisplayName("Then we can print report (empty)")
            public void testReport() {
                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));

                clp.printReport();

                assertTrue(outputStreamCaptor.toString()
                        .startsWith("\tServer: [" + clp.getServer() + "]"));

                // restore standard output
                System.setOut(standardOut);
            }

        }

        @Nested
        @DisplayName("When a good set of command line arguments were parsed")
        public class testGoodParsing {
            private final String[] goodString = new String[]
                    { "-s", "ftp.ebi.ac.uk"
                            , "-r", "/pub/databases/opentargets/platform/21.11/output/etl/json/"
                            , "-l", "./data/"
                            , "-d", "diseases"};

            private final CommandLineParameters clp = new CommandLineParameters();

            @BeforeEach
            public void setUp() throws ParseException {
                clp.parse(goodString);
            }

            @Test
            @DisplayName("Then we have all parameters set correctly")
            public void testParamsAreCorrect() {
                assertAll(
                        () -> assertEquals(clp.getServer(), URI.create("ftp.ebi.ac.uk"))
                        , () -> assertEquals(clp.getRemoteBase(), Path.of
                                ("/pub/databases/opentargets/platform/21.11/output/etl/json/"))
                        , () -> assertEquals(clp.getLocalBase(), Path.of("./data/"))
                        , () -> assertEquals(clp.getDir(), Path.of("diseases")));
            }

            @Test
            @DisplayName("Then we can print report (with correct values)")
            public void testReportIsCorrect() {
                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));

                clp.printReport();

                assertTrue(outputStreamCaptor.toString()
                        .startsWith("\tServer: [" + clp.getServer() + "]"));

                // restore standard output
                System.setOut(standardOut);
            }
        }

        @Nested
        @DisplayName("When a bad set of command line arguments were parsed")
        public class testBadParsing {
            private final String[] goodString = new String[]
                    { "-s", "ftp.ebi.ac.uk"
                            , "-wrong_arg", "/pub/databases/opentargets/platform/21.11/output/etl/json/"
                            , "-l", "./data/"
                            , "-d", "diseases"};

            private final CommandLineParameters clp = new CommandLineParameters();

            @Test
            @DisplayName("Then we have a ParseException")
            public void testParseException() {
                assertThrows(ParseException.class, () -> clp.parse(goodString));
            }

        }
    }
}










