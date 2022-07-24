package FtpUtil;

import FtpUtil.cli.CommandLineParameters;
import org.apache.commons.cli.ParseException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

public class FtpUtil {
    private static long countDownloadedFiles = 0;

    public static void main(String... args) {
        //
        // understand user defined options
        //

        var clp = new CommandLineParameters();

        try {
            clp.parse(args);
        } catch (ParseException ex) {
            System.out.println("Parsing of command line arguments failed: "
                    + ex.getMessage());

            System.out.println();

            // just a bit annoying...
            // leave it for now...
            clp.printHelp();

            System.exit(1);
        }


        if (Arrays.stream(args).count() < 4) {
            System.out.println(("Four arguments are required: server, remoteBase, localBase, dir"));
            System.exit(0);
        }

        System.out.println();
        System.out.println("Proceeding with the following parameters");
        System.out.println("\tServer: [" + clp.getServer() + "]");
        System.out.println("\tRemote: [" + clp.getRemoteBase() + "]");
        System.out.println("\tLocal:  [" + clp.getLocalBase() + "]");
        System.out.println("\tDir:    [" + clp.getDir() + "]");

        //
        // resolve directories
        //
        Path remotePath = clp.getRemoteBase().resolve(clp.getDir());
        Path localPath = clp.getLocalBase().resolve(clp.getDir());

        //
        // ensure we have the local dir structure in place
        //

        try {
            Files.createDirectories(localPath);
        } catch (IOException ex) {
            System.out.format("Unable to create local directory: [%s] reason: [%s]"
                    , localPath.toString(), ex.getMessage());
            System.out.println();
            System.exit(0);
        }

        //
        // run the job
        //
        ConnectAndDownload(clp.getServer(), remotePath, localPath);
    }

    public static void ConnectAndDownload
            (URI server, Path remoteDir, Path localDir) {

        // download files from FTP server
        try(var client = FtpClient.getClient(server.toString(), new FTPClient());) {
            var files = client.listFiles(remoteDir);

            // have the same predicate for counting and downloading
            Predicate<FTPFile> fileFilter = FTPFile::isFile;

            // prepare to measure elapsed time
            long startTime = System.nanoTime();

            // divider from previous outputs
            System.out.println();

            // go through the file list and download each one
            files.stream()
                    .filter(fileFilter)
                    .forEach(f -> {
                        System.out.format("Downloading (%d of %d):[%s]"
                                , ++countDownloadedFiles, files.size(), f.getName());
                        System.out.println();

                            try (var out = new BufferedOutputStream(new FileOutputStream
                                    (localDir.resolve(f.getName()).toFile()))) {

                                client.downloadFile(remoteDir.resolve(f.getName()), out);
                            } catch (IOException ex) {
                                throw new RuntimeException("FTP file download has failed: ", ex);
                            }
                    });

            // report elapsed time
            long elapsedTime = System.nanoTime() - startTime;

            System.out.println();
            System.out.format("elapsed time: %.0f (ms)",elapsedTime * 1e-6);
            System.out.println();

        } catch (IOException ex) {
            System.out.println("Communication with FTP server failed...");
            ex.printStackTrace();
        }
    }
}
