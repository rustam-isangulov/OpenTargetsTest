package FtpUtil;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FtpClient implements Closeable {
    private final FTPClient ftp;
    private final String server;


    public static FtpClient getClient(String serverAddress, FTPClient ftp) throws IOException {
        // default port
        int port = 21;
        // username for public access data
        String user = "anonymous";
        // password for anonymous users
        String password = "";

        FtpClient client = new FtpClient(serverAddress, ftp);

        client.open(port, user, password);

        return client;
    }

    public static FtpClient getClient(String server, FTPClient ftp, int port, String user, String password) throws IOException {
        FtpClient client = new FtpClient(server, ftp);

        client.open(port, user, password);

        return client;
    }

    public FtpClient(String serverAddress, FTPClient ftp) {
        this.server = serverAddress;
        this.ftp = ftp;
    }

    public List<FTPFile> listFiles(Path path) throws IOException {
        FTPFile[] files = ftp.listFiles(path.toString());
        return Arrays.stream(files)
                .collect(Collectors.toList());
    }

    public void downloadFile(Path remoteFile, OutputStream out) throws IOException {

        ftp.retrieveFile(remoteFile.toString(), out);
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

    private FtpClient open(int port, String user, String password) throws IOException {
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

        // check for connection failures
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            ftp.disconnect();
            throw new IOException
                    ("Unable to login to FTP Server: " + server
                            + " port: " + port);
        }

        // passive mode to be able to work from inside VMs
        ftp.enterLocalPassiveMode();

        // now ready to access files and dirs
        return this;
    }
}
