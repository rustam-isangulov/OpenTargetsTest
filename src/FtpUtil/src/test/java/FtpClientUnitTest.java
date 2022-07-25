import FtpUtil.FtpClient;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FtpClientUnitTest {

    @Nested
    @DisplayName("When open default ftp connection is successful")
    public class openIfSuccessful {
        private FTPClient mockFTPClient = mock(FTPClient.class);

        private FtpClient client;

        @BeforeEach
        public void setUp() throws IOException {
            // set up a happy path for connecting / login sequence
            when(mockFTPClient.getReplyCode())
                    .thenReturn(230);
            when(mockFTPClient.login(anyString(), anyString()))
                    .thenReturn(true);

            client = FtpClient.getClient
                    ("localhost", mockFTPClient);
        }

        @Test
        @DisplayName("Test open connection sequence for default port/user")
        public void testFTPOpenDefault() throws IOException {
            // open connection should have three steps:
            // connect to server @ port
            verify(mockFTPClient).connect("localhost", 21);
            // log in with username anonymous and no password
            verify(mockFTPClient).login("anonymous", "");
            // switch to passive mode to be able to work from VMs
            // (behind NATs and port mapping)
            verify(mockFTPClient).enterLocalPassiveMode();
        }

        @Test
        @DisplayName("Test close sequence")
        public void testFTPClose() throws IOException {
            // close (as in Closeable interface)
            client.close();

            // order of calls verification
            InOrder inOrder = inOrder(mockFTPClient);

            inOrder.verify(mockFTPClient).logout();
            inOrder.verify(mockFTPClient).disconnect();
        }

        @Test
        @DisplayName("Test list files in a directory")
        public void testFTPListFiles() throws IOException {
            FTPFile[] ftpFiles = new FTPFile[1];
            ftpFiles[0] = new FTPFile();
            ftpFiles[0].setName("fileOne.txt");

            // mock FTPClient response for the list command
            when(mockFTPClient.listFiles(anyString()))
                    .thenReturn(ftpFiles);

            // get the list through FtpClient
            List<FTPFile> list = client.listFiles(Path.of("/any/path"));

            assertAll("Test to list a single file"
            , () -> assertEquals(1, list.size())
            , () -> assertTrue(list.get(0).getName().equals("fileOne.txt")
                            , () -> "File name should match the expected one"));
        }

        @Test
        @DisplayName("Test downloading a file call")
        public void testFTPDownloadFile(@TempDir Path dataDir) throws IOException {
            // prepare an output stream to copy file content
            var out = new FileOutputStream
                    (dataDir.resolve("test.txt").toFile());

            // try to download a file with the given path
            client.downloadFile(Path.of("any/path"), out);

            verify(mockFTPClient).retrieveFile("any/path", out);
        }
    }

    @Nested
    @DisplayName("Test unsuccessful open connection cases")
    public class unsuccessfulCases {
        private FTPClient mockFTPClient = mock(FTPClient.class);

        @Test
        @DisplayName("Test connect problem behaviour")
        public void testConnectProblem() throws IOException {
            // set up a troublesome path for the connecting sequence
            when(mockFTPClient.getReplyCode())
                    .thenReturn(534);

            Throwable ex = assertThrows(IOException.class
                    , () -> FtpClient.getClient("localhost", mockFTPClient)
            , () -> "FtpClient should throw an exception when ftp return code is not positive");
        }

        @Test
        @DisplayName("Test login problem behaviour")
        public void testLoginProblem() throws IOException {
            // set up a happy path for the connecting sequence
            when(mockFTPClient.getReplyCode())
                    .thenReturn(230);

            // but bad response for the login call
            when(mockFTPClient.login(anyString(), anyString()))
                    .thenReturn(false);

            Throwable ex = assertThrows(IOException.class
                    , () -> FtpClient.getClient("localhost", mockFTPClient)
                    , () -> "FtpClient should throw an exception when ftp login is not successful");
        }
    }

    @Nested
    @DisplayName("When connecting with non-default port and user")
    public class connectDetailed {
        private FTPClient mockFTPClient = mock(FTPClient.class);

        @Test
        @DisplayName("Test open connection with port / user / password")
        public void testFTPOpenDetailed() throws IOException {
            // set up a happy path for connecting / login sequence
            when(mockFTPClient.getReplyCode())
                    .thenReturn(230);
            when(mockFTPClient.login(any(), any()))
                    .thenReturn(true);

            FtpClient client = FtpClient.getClient
                    ("ftp.server", mockFTPClient
                            , 22, "userOne", "passwordSecret");


            // open connection should have three steps:
            // connect to server @ port
            verify(mockFTPClient).connect("ftp.server", 22);
            // log in with username anonymous and no password
            verify(mockFTPClient).login("userOne", "passwordSecret");
            // switch to passive mode to be able to work from VMs
            // (behind NATs and port mapping)
            verify(mockFTPClient).enterLocalPassiveMode();
        }
    }
}












