package FtpUtil;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FtpClient implements Closeable {
    public enum ATTRIBUTES { VERBOSE }

    private final FTPClient ftp = new FTPClient();
    private final String server;

    public FtpClient(String server, ATTRIBUTES... attr) {
        this.server = server;

        // add detailed output from FTPClient
        if (Arrays.stream(attr).anyMatch(a -> a == ATTRIBUTES.VERBOSE)) {
            ftp.addProtocolCommandListener
                    (new PrintCommandListener(new PrintWriter(System.out)));
        }
    }

    public void downloadFile(Path remoteFile, OutputStream out) throws IOException {

        ftp.retrieveFile(remoteFile.toString(), out);
    }

    public List<FTPFile> listFiles(Path path) throws IOException {
        FTPFile[] files = ftp.listFiles(path.toString());
        return Arrays.stream(files)
                .collect(Collectors.toList());
    }

    public FtpClient open() throws IOException {
        // default port
        int port = 21;
        // username for public access data
        String user = "anonymous";
        // password for anonymous users
        String password = "";

        return this.open(port, user, password);
    }

    public FtpClient open(int port, String user, String password) throws IOException {
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

        // passive mode to be able to work from inside VMs
        ftp.enterLocalPassiveMode();

        // now ready to access files and dirs
        return this;
    }

    public int getReplyCode() throws IOException {
        return ftp.getReplyCode();
    }

    public boolean isConnected() throws IOException {
        return ftp.isConnected();
    }

    @Override
    public void close() throws IOException {
        ftp.logout();
        ftp.disconnect();
    }
}
