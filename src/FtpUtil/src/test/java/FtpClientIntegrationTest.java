import FtpUtil.FtpClient;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockftpserver.core.NotLoggedInException;
import org.mockftpserver.core.command.CommandHandler;
import org.mockftpserver.core.command.ConnectCommandHandler;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockftpserver.fake.filesystem.FileSystem;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FtpClientIntegrationTest {
    private FakeFtpServer fakeFtpServer;
    //private FtpClient ftpClient;

    private Path longFilePath = Path.of
            ("/pub/databases/opentargets/platform/21.11/output/" +
            "etl/json/diseases/part-00000-773deead-54e9-4934-b648-b26a4bbed763-c000.json");
    private String longFileContent = "{\"id\":\"HP_0000031\",\"code\":\"http://purl.obolibrary.org/obo/HP_0000031\",\"dbXRefs\":" +
            "[\"UMLS:C0014534\",\"SNOMEDCT_US:31070006\",\"MSH:D004823\"],\"description\":" +
            "\"The presence of inflammation of the epididymis.\",\"name\":\"Epididymitis\",\"parents\":" +
            "[\"HP_0012649\",\"HP_0000022\"],\"ancestors\":" +
            "[\"HP_0000022\",\"HP_0000078\",\"EFO_0000651\",\"HP_0012649\",\"HP_0012647\",\"HP_0000118\",\"HP_0002715\"]," +
            "\"descendants\":[],\"children\":[],\"therapeuticAreas\":[\"EFO_0000651\"],\"ontology\":" +
            "{\"isTherapeuticArea\":false,\"leaf\":true,\"sources\"" +
            ":{\"url\":\"http://purl.obolibrary.org/obo/HP_0000031\",\"name\":\"HP_0000031\"}}}";


    @BeforeEach
    public void setUp() throws IOException {
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
        fileSystem.add(new FileEntry("/data/test.txt","test text"));
        fileSystem.add(new DirectoryEntry("/pub/databases/opentargets/platform/21.11/output/etl/json/"));
        fileSystem.add(new DirectoryEntry(longFilePath.getParent().toString()));
        fileSystem.add(new FileEntry(longFilePath.toString(), longFileContent));

        fakeFtpServer.setFileSystem(fileSystem);;
        fakeFtpServer.setSystemName("Unix");

        fakeFtpServer.start();

        // ftp "server" should be up and running at this point
    }


    @AfterEach
    public void tearDown() throws IOException {
        fakeFtpServer.stop();
    }


    @Test
    @DisplayName("Check if requests to list files in a remote directory work.")
    public void givenRemoteDir_whenListingFiles_thenItIsContainedInTheList() throws IOException {

        try (var ftpClient = FtpClient.getClient("localhost")) {
            List<FTPFile> files = ftpClient.listFiles(Path.of
                    ("/pub/databases/opentargets/platform/21.11/output/etl/json/diseases/"));

            assertAll("Test to list and retrieve a single file"
                    , () -> assertTrue(1 == files.size(), () -> "Number of files int he dir is 1!")
                    , () -> assertEquals(longFilePath.getFileName().toString(), files.get(0).getName()));
        }
    }


    @Test
    @DisplayName("Check if downloading of a remote file works.")
    public void givenRemoteFile_whenDownload_thenContentIsAvailableAsStream(@TempDir Path localDir) throws IOException {

        try (var ftpClient = FtpClient.getClient("localhost")) {
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
    @DisplayName("Check if we get a positive reply after connecting to the server.")
    public void givenNewlyConnectedFtpClient_whenCheckingLastReply_then221Connected() throws IOException {
        int loggedInProceedCode = 230;

        try (var client = FtpClient.getClient("localhost")) {

            assertTrue(loggedInProceedCode == client.getReplyCode()
                    , () -> "FTP server should return code 230 after connect/login.");
        }
    }


    @Test
    @DisplayName("Check how the client reacts to the wrong user not being logged in.")
    public void givenNewlyCreatedFTPClient_whenBadUserName_then530Error() throws IOException {
        String badUserName = "nonympus";
        int notLoggedInUserFTPCode = 530;

        try(var newClient = FtpClient.getClient
                ("localhost",fakeFtpServer.getServerControlPort(), badUserName, "" )) {

            // try tp list files while not logged in
            newClient.listFiles(Path.of("/"));

            assertTrue(notLoggedInUserFTPCode == newClient.getReplyCode()
                    , () -> "FTP server should return code 530 for not logged in users.");
        }
    }


    @Test
    @DisplayName("Check how the client reacts to a negative server code from at connect.")
    public void givenNewlyCreatedFTPClient_whenBadResponseOnConnect_thenException() throws IOException {

        // change response to the next Connect request
        ConnectCommandHandler handler = (ConnectCommandHandler)fakeFtpServer.getCommandHandler("Connect");
        handler.setReplyCode(534);
        handler.setReplyMessageKey("Request denied for policy reasons.");
        handler.setReplyText("Request denied for policy reasons.");

        // test connect() method
        assertThrows(IOException.class
                , () -> FtpClient.getClient("localhost", fakeFtpServer.getServerControlPort(), "anonymous", "")
                , () -> "Return code 534 (not a positive completion) should cause an IOException");
    }


    @Test
    @DisplayName("Check the client (in verbose mode) properly closes connection to the server. ")
    public void givenNewlyCreatedFTPClientVerbose_whenClose_thenItDisconnects() throws IOException {
        FtpClient clientRef;

        try (var newClient = FtpClient.getClient
                ("localhost", FtpClient.ATTRIBUTES.VERBOSE)) {
            clientRef = newClient;
        }

        assertFalse(clientRef.isConnected()
                , () -> "FTP server should be disconnected after close() call.");
    }
}
