package FtpUtil;

import FtpUtil.cli.CommandLineParameters;
import org.apache.commons.cli.ParseException;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class FtpUtil {

    public static void main(String... args) {

        var clp = new CommandLineParameters();

        try {
            // understand user defined options
            clp.parse(args);

            // ready to go
            System.out.println();
            System.out.println("Proceeding with the following parameters");
            clp.printReport();

            // prepare necessary directory structure
            Files.createDirectories(clp.getLocalBase().resolve(clp.getDir()));

        } catch (ParseException ex) {
            System.out.println("Parsing of command line arguments failed: "
                    + ex.getMessage());

            System.out.println();
            // just a bit annoying...
            // leave it for now...
            clp.printHelp();

            // no reasonable recovery from here...
            return;
        } catch (IOException ex) {
            System.out.format("Unable to create local directory: [%s] reason: [%s]"
                    , clp.getLocalBase().resolve(clp.getDir()), ex.getMessage());
            System.out.println();

            // no reasonable recovery from here...
            return;
        }

        //
        // create a new runner with all required info
        //
        FtpUtil utilityRunner = new FtpUtil
                (clp.getServer()
                        , clp.getRemoteBase()
                        , clp.getDir()
                        , clp.getLocalBase());

        //
        // run the job
        //
        utilityRunner.ConnectAndDownload();
    }

    private final URI server;
    private final Path fullRemotePath;
    private final Path fullLocalPath;

    public FtpUtil(URI server, Path remoteBase, Path dataDir, Path localBase) {
        this.server = server;

        fullRemotePath = remoteBase.resolve(dataDir);
        fullLocalPath = localBase.resolve(dataDir);
    }

    public void ConnectAndDownload () {
        // divider from previous outputs
        System.out.println();

        // prepare to measure elapsed time
        long startTime = System.nanoTime();

        // download files from FTP server
        try(var client = FtpClient.getClient(server.toString(), new FTPClient())) {

            // provide output to copy a remote file content
            Function<Path, OutputStream> outputProvider =
                    file -> {
                        try {
                            return new BufferedOutputStream(
                                    new FileOutputStream
                                            (fullLocalPath.resolve(file).toFile()));
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    };

            // react to FtpClient updates
            Consumer<String> downloadProgressEvent = System.out::println;

            // download all files form the remoteDir
            client.downloadAllFiles(fullRemotePath, outputProvider, downloadProgressEvent);

        } catch (IOException ex) {
            System.out.println("Communication with FTP server failed...");
            ex.printStackTrace();
            return;
        }

        // report elapsed time
        long elapsedTime = System.nanoTime() - startTime;

        System.out.println();
        System.out.format("elapsed time: %.0f (ms)",elapsedTime * 1e-6);
        System.out.println();
    }

    public Path getFullRemotePath() {
        return fullRemotePath;
    }

    public Path getFullLocalPath() {
        return fullLocalPath;
    }
}
