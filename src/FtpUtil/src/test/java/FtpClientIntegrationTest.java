import FtpUtil.FtpClient;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockftpserver.core.command.ConnectCommandHandler;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockftpserver.fake.filesystem.FileSystem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FtpClientIntegrationTest {
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


    @Nested
    @DisplayName("Given an FTP server with directories and files")
    class ftpServer {

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
            fileSystem.add(new DirectoryEntry("/pub/databases/opentargets/platform/21.11/output/etl/json/"));
            fileSystem.add(new DirectoryEntry(longFilePath.getParent().toString()));
            fileSystem.add(new FileEntry(longFilePath.toString(), longFileContent));

            fakeFtpServer.setFileSystem(fileSystem);

            fakeFtpServer.setSystemName("Unix");

            fakeFtpServer.start();

            // ftp "server" should be up and running at this point
        }


        @AfterEach
        public void tearDown() {
            fakeFtpServer.stop();
        }

        @Nested
        @DisplayName("When we connected and logged in")
        class directoryWithFiles {

            @Test
            @DisplayName("Then we can list all files in the directory")
            public void givenRemoteDir_whenListingFiles_thenItIsContainedInTheList() throws IOException {

                try (var ftpClient = FtpClient.getClient("localhost", new FTPClient())) {
                    List<FTPFile> files = ftpClient.listFiles(Path.of
                            ("/pub/databases/opentargets/platform/21.11/output/etl/json/diseases/"));

                    assertAll("Test to list and retrieve a single file"
                            , () -> assertTrue(1 == files.size(), () -> "Number of files int the dir is 1!")
                            , () -> assertEquals(longFilePath.getFileName().toString(), files.get(0).getName()));
                }
            }


            @Test
            @DisplayName("Then we can download content of a specific file")
            public void givenRemoteFile_whenDownload_thenContentIsAvailableAsStream
                    (@TempDir Path localDir) throws IOException {

                try (var ftpClient = FtpClient.getClient("localhost", new FTPClient())) {
                    try (var out = new BufferedOutputStream(new FileOutputStream
                            (localDir.resolve(longFilePath.getFileName()).toFile()))) {

                        ftpClient.downloadFile(longFilePath, out);
                    }

                    assertTrue(Files.readAllLines(localDir.resolve(longFilePath.getFileName())).stream()
                                    .collect(Collectors.joining()).startsWith(longFileContent)
                            , () -> "Content of downloaded file has to mach the pre-defined string.");
                }
            }

            @Test
            @DisplayName("Then we can download all files from a specific directory")
            public void givenRemoteDir_whenDownloadAll_thenContentCopiedIntoStreams
                    (@TempDir Path localDir) throws IOException {

                Function<Path, OutputStream> outputProvider =
                        file -> {
                            try {
                                return new BufferedOutputStream(
                                        new FileOutputStream
                                                (localDir.resolve(file).toFile()));
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        };

                Consumer<String> downloadProgressEvent = message -> {};

                try (var ftpClient = FtpClient.getClient
                        ("localhost", new FTPClient())) {

                    ftpClient.downloadAllFiles
                            (longFilePath.getParent(), outputProvider, downloadProgressEvent);

                    assertTrue(Files.exists
                                    (localDir.resolve(longFilePath.getFileName()))
                            , () -> "A single pre-defined file should be downloaded");
                }

            }
        }


        @Nested
        @DisplayName("When we login with a wrong user or password")
        class loginIsRefused {

            @Test
            @DisplayName("Then the client throws Unable to login... exception")
            public void givenNewlyCreatedFTPClient_whenBadUserName_thenException() {
                String badUserName = "nonympus";

                // test connect() method
                Throwable exception = assertThrows(IOException.class
                        , () -> FtpClient.getClient("localhost", new FTPClient(), fakeFtpServer.getServerControlPort(), badUserName, "")
                        , () -> "Return code 501 (Syntax error...) should cause an IOException");

                assertTrue(exception.getMessage().startsWith("Unable to login")
                        , () -> "FtpClient should throw an exception with Unable to login message in case of a negative login reply");
            }
        }


        @Nested
        @DisplayName("When server refuses our attempt to connect")
        class connectionIsRefused {

            @BeforeEach
            public void setUp() {
                // change response to the next Connect request
                ConnectCommandHandler handler = (ConnectCommandHandler) fakeFtpServer.getCommandHandler("Connect");
                handler.setReplyCode(534);
                handler.setReplyMessageKey("Request denied for policy reasons.");
                handler.setReplyText("Request denied for policy reasons.");
            }

            @Test
            @DisplayName("Then the client throws Unable to connect... exception")
            public void givenNewlyCreatedFTPClient_whenBadResponseOnConnect_thenException() {

                // test connect() method
                Throwable exception = assertThrows(IOException.class
                        , () -> FtpClient.getClient("localhost", new FTPClient(), fakeFtpServer.getServerControlPort(), "anonymous", "")
                        , () -> "Return code 534 (not a positive completion) should cause an IOException");

                assertTrue(exception.getMessage().startsWith("Unable to connect")
                        , () -> "FtpClient should throw an exception with Unable to connect message in case of a negative connect reply");
            }

            @AfterEach
            public void resetServer() {
                // this is not strictly necessary
                // but will help avoid errors if we switch
                // to a single FTP server for all tests
                fakeFtpServer.setCommandHandler("Connect", new ConnectCommandHandler());
            }
        }
    }
}
