package FtpUtil;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class FtpClient implements Closeable {
    public enum ATTRIBUTES { VERBOSE }
    private final FTPClient ftp = new FTPClient();

    public static FtpClient of(String server, ATTRIBUTES... attr) throws IOException {
        // default port
        int port = 21;
        // username for public access data
        String user = "anonymous";
        // password for anonymous users
        String password = "";

        FtpClient client = new FtpClient(attr);
        // make the client ready to deal with file transfer requests
        client.open(server, port, user, password);

        return client;
    }

    public void downloadFile(Path remoteDir, Path remoteFile, Path localDir) {
        try(var out = new BufferedOutputStream(new FileOutputStream
                            (localDir.resolve(remoteFile).toFile()))) {
            ftp.retrieveFile(remoteDir.resolve(remoteFile).toString(), out);
        } catch (IOException ex) {
            throw new RuntimeException("Unable download file:[" + remoteFile + "]", ex);
        }
    }

    public Collection<FTPFile> listFiles(Path path) throws IOException {
        FTPFile[] files = ftp.listFiles(path.toString());
        return Arrays.stream(files)
                .collect(Collectors.toList());
    }

    private void open(String server, int port, String user, String password) throws IOException {
        // connect
        ftp.connect(server, port);

        // check for connection failures
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            ftp.disconnect();
            throw new IOException
                    ("Unable to connect to FTP Server: " + server
                    + " port: " + port);
        }

        // login
        ftp.login(user, password);

        // now ready to access files and dirs
    }

    private FtpClient(ATTRIBUTES... attr) {
        // add detailed output from FTPClient
        if (Arrays.stream(attr).anyMatch(a -> a == ATTRIBUTES.VERBOSE)) {
            ftp.addProtocolCommandListener
                    (new PrintCommandListener(new PrintWriter(System.out)));
        }
    }


    @Override
    public void close() throws IOException {
        ftp.logout();
        ftp.disconnect();
    }
}
