package FtpUtil;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilters;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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

    public static FtpClient getClient(String serverAddress, FTPClient ftp
            , int port, String user, String password) throws IOException {
        FtpClient client = new FtpClient(serverAddress, ftp);

        client.open(port, user, password);

        return client;
    }

    public List<FTPFile> listFiles(Path path) throws IOException {
        FTPFile[] files = ftp.listFiles(path.toString());
        return Arrays.stream(files)
                .collect(Collectors.toList());
    }

    public void downloadFile(Path remoteFile, OutputStream out) throws IOException {
        ftp.retrieveFile(remoteFile.toString(), out);
    }

    public void downloadAllFiles
            (Path remoteDir
                    , Function<Path, OutputStream> outputProvider
                    , Consumer<String> downloadProgressReporter) throws IOException {
        var files = ftp.listFiles(remoteDir.toString());

        var filesList = Arrays.stream(files)
                .filter(FTPFile::isFile)
                .collect(Collectors.toList());

        for(int i = 0; i < filesList.size(); i++) {
            downloadProgressReporter.accept(String.format
                    ("Downloading (%d of %d):[%s]", i+1, filesList.size(), filesList.get(i).getName()));

            try (var out = outputProvider.apply(Path.of(filesList.get(i).getName()))) {
                ftp.retrieveFile(remoteDir.resolve(filesList.get(i).getName()).toString(), out);
            }
        }
    }

    @Override
    public void close() throws IOException {
        ftp.logout();
        ftp.disconnect();
    }

    private FtpClient(String serverAddress, FTPClient ftp) {
        this.server = serverAddress;
        this.ftp = ftp;
    }

    private void open(int port, String user, String password) throws IOException {
        // connect
        ftp.connect(server, port);

        // check for connection failures
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            ftp.disconnect();
            throw new IOException
                    ("Unable to connect to FTP Server: " + server
                    + " port: " + port);
        }

        // check for login failures
        if (!ftp.login(user, password)) {
            ftp.disconnect();
            throw new IOException
                    ("Unable to login to FTP Server: " + server
                            + " port: " + port);
        }

        // passive mode to be able to work from inside VMs
        ftp.enterLocalPassiveMode();

        // now ready to access files and dirs
    }
}
