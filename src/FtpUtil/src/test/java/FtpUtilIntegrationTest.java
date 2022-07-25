import FtpUtil.FtpUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockftpserver.core.command.ConnectCommandHandler;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FtpUtilIntegrationTest {

    //
    // FTP server setup
    //
    private FakeFtpServer fakeFtpServer;
    private final Path longFilePath = Path.of
            ("/pub/databases/opentargets/platform/21.11/output/" +
                    "etl/json/diseases/part-00000-773deead-54e9-4934-b648-b26a4bbed763-c000.json");
    private final String longFileContent = "{\"id\":\"HP_0000031\",\"code\":\"http://purl.obolibrary.org/obo/HP_0000031\",\"dbXRefs\":" +
            "[\"UMLS:C0014534\",\"SNOMEDCT_US:31070006\",\"MSH:D004823\"],\"description\":" +
            "\"The presence of inflammation of the epididymis.\",\"name\":\"Epididymitis\",\"parents\":" +
            "[\"HP_0012649\",\"HP_0000022\"],\"ancestors\":" +
            "[\"HP_0000022\",\"HP_0000078\",\"EFO_0000651\",\"HP_0012649\",\"HP_0012647\",\"HP_0000118\",\"HP_0002715\"]," +
            "\"descendants\":[],\"children\":[],\"therapeuticAreas\":[\"EFO_0000651\"],\"ontology\":" +
            "{\"isTherapeuticArea\":false,\"leaf\":true,\"sources\"" +
            ":{\"url\":\"http://purl.obolibrary.org/obo/HP_0000031\",\"name\":\"HP_0000031\"}}}";


    @BeforeEach
    public void setUp() {
        fakeFtpServer = new FakeFtpServer();

        // setup default account
        UserAccount anonymous = new UserAccount();
        anonymous.setPasswordRequiredForLogin(false);
        anonymous.setUsername("anonymous");
        anonymous.setHomeDirectory("/");

        fakeFtpServer.addUserAccount(anonymous);

        // setup sample directory and files structure
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fileSystem.add(new FileEntry("/data/test.txt", "test text"));
        //fileSystem.add(new DirectoryEntry("/pub/databases/opentargets/platform/21.11/output/etl/json/"));
        fileSystem.add(new DirectoryEntry(longFilePath.getParent().toString()));
        fileSystem.add(new FileEntry(longFilePath.toString(), longFileContent));

        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.setSystemName("Unix");

        fakeFtpServer.start();
    }

    @AfterEach
    public void tearDown() {
        fakeFtpServer.stop();
    }

    @Nested
    @DisplayName("Given wrong local directory structure")
    public class testBadSetup {
        private final URI server = URI.create("localhost");
        private final Path remoteBase = Path.of("/pub/databases/opentargets/platform/21.11/output/etl/json/");
        private final Path dataDir = Path.of("diseases");
        private final Path localBase = Path.of("/null");

        @Nested
        @DisplayName("When we create an instance of FtpUtil")
        public class testInstance {
            private FtpUtil ftpUtil;

            @BeforeEach
            public void setUp() {
                ftpUtil = new FtpUtil(server, remoteBase, dataDir, localBase);
            }

            @Test
            @DisplayName("The the instance throws FileNotFoundException")
            public void testFileNotFound() {
                // FileNotFoundException is packages into RuntimeException

                // assert Runtime first
                Throwable ex = assertThrows
                        (RuntimeException.class, () -> ftpUtil.ConnectAndDownload());
                // extract IOException
                assertEquals(FileNotFoundException.class, ex.getCause().getClass());
            }
        }
    }

    @Nested
    @DisplayName("Given correct server uri, directories and accessible ftp server")
    public class testCorrectSetup {
        private final URI server = URI.create("localhost");
        private final Path remoteBase = Path.of("/pub/databases/opentargets/platform/21.11/output/etl/json/");
        private final Path dataDir = Path.of("diseases");
        @TempDir
        private Path localBase;


        @Nested
        @DisplayName("When we create an instance of FtpUtil")
        public class testInstance {
            private FtpUtil ftpUtil;

            @BeforeEach
            public void setUp() {
                ftpUtil = new FtpUtil(server, remoteBase, dataDir, localBase);
            }

            @Test
            @DisplayName("Then the instance has correct full remote and local paths")
            public void testCorrectPaths() {

                assertAll(
                        () -> assertEquals(remoteBase.resolve(dataDir), ftpUtil.getFullRemotePath())
                        , () -> assertEquals(localBase.resolve(dataDir), ftpUtil.getFullLocalPath())
                );
            }

            @Test
            @DisplayName("Then we can download content of a specific remote file")
            public void testCorrectDownload() throws IOException {
                //
                // prepare necessary directory structure
                //
                Files.createDirectories(ftpUtil.getFullLocalPath());

                // download
                ftpUtil.ConnectAndDownload();

                // create the local file path given pre-defined remote file name
                Path downloadedFile = ftpUtil.getFullLocalPath().resolve(longFilePath.getFileName());

                long filesCount;
                String fileContent;

                try (var listStream = Files.list(ftpUtil.getFullLocalPath());
                     var reader = Files.newBufferedReader(downloadedFile)) {

                    filesCount = listStream.count();
                    fileContent = reader.readLine();
                }

                // assert both number of files and content
                assertAll(
                        () -> assertEquals(1, filesCount)
                        , () -> assertEquals(fileContent, longFileContent)
                );
            }
        }


        @Nested
        @DisplayName("When we run the main method")
        public class testMain {
            @Test
            @DisplayName("The utility downloads correct files")
            public void testMainCorrectWork() throws IOException {
                String[] goodString = new String[]
                        { "-s", server.toString()
                                , "-r", remoteBase.toString()
                                , "-l", localBase.toString()
                                , "-d", dataDir.toString()};


                FtpUtil.main(goodString);

                // create the local file path given pre-defined remote file name
                Path downloadedFile = localBase.resolve(dataDir).resolve(longFilePath.getFileName());

                //
                // assert both number of files and content
                //

                long filesCount;
                String fileContent;

                try (var listStream =  Files.list(localBase.resolve(dataDir));
                var reader = Files.newBufferedReader(downloadedFile)) {
                    filesCount = listStream.count();
                    fileContent = reader.readLine();
                }

                assertAll(
                        () -> assertEquals(1, filesCount)
                        , () -> assertEquals(fileContent, longFileContent)
                );
            }

            @Test
            @DisplayName("The utility downloads correct files")
            public void testMainFTPError() {
                // change response to the next Connect request
                ConnectCommandHandler handler = (ConnectCommandHandler) fakeFtpServer.getCommandHandler("Connect");
                handler.setReplyCode(534);
                handler.setReplyMessageKey("Request denied for policy reasons.");
                handler.setReplyText("Request denied for policy reasons.");

                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));



                String[] goodString = new String[]
                        { "-s", server.toString()
                                , "-r", remoteBase.toString()
                                , "-l", localBase.toString()
                                , "-d", dataDir.toString()};

                FtpUtil.main(goodString);

                assertTrue(outputStreamCaptor.toString()
                        .contains("Communication with FTP server failed..."));



                // restore standard output
                System.setOut(standardOut);

                // back to normal response
                fakeFtpServer.setCommandHandler("Connect", new ConnectCommandHandler());
            }
        }

        @Nested
        @DisplayName("When we run the main method with bad arguments")
        public class testMainBad {
            @Test
            @DisplayName("The utility quits with Parsing... error message for bad arguments")
            public void testMainBadArgumentError() {
                String[] badString = new String[]
                        { "-s", server.toString()
                                , "-badargument", remoteBase.toString()
                                , "-l", localBase.toString()
                                , "-d", dataDir.toString()};

                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));

                FtpUtil.main(badString);

                assertTrue(outputStreamCaptor.toString()
                        .contains("Parsing of command line arguments failed"));

                // restore standard output
                System.setOut(standardOut);

            }

            @Test
            @DisplayName("The utility quits with Unable to create... error message for bad local directory")
            public void testMainCreateDirError() {
                String[] badString = new String[]
                        { "-s", server.toString()
                                , "-r", remoteBase.toString()
                                , "-l", "/null"
                                , "-d", dataDir.toString()};

                // redirect standard output stream
                final PrintStream standardOut = System.out;
                final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
                System.setOut(new PrintStream(outputStreamCaptor));

                FtpUtil.main(badString);

                assertTrue(outputStreamCaptor.toString()
                        .contains("Unable to create"));

                // restore standard output
                System.setOut(standardOut);

            }
        }
    }
}
