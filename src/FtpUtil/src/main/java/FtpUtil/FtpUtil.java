package FtpUtil;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

public class FtpUtil {

    private static long totalFilesToDownload = 0;
    private static long countDownloadedFiles = 0;


    public static void main(String... args) {
        System.out.println("FtpUtil is starting...");

        if (Arrays.stream(args).count() < 4) {
            System.out.println(("Four arguments are required: server, remoteBase, localBase, dir"));
            System.exit(0);
        }

        System.out.println();
        System.out.println("Server: [" + args[0] + "]");
        System.out.println("Remote base: [" + args[1] + "]");
        System.out.println("Local base: [" + args[2] + "]");
        System.out.println("Dir: [" + args[3] + "]");

        ConnectionTest(args[0], args[1], args[2], args[3]);

        System.out.println("FtpUtil has finished.");
    }

    public static void ConnectionTest
            (String strServer, String strRemoteBase, String strLocalBase, String strDir ) {
        String server = strServer; //= "ftp.ebi.ac.uk";

        Path remoteDirBase = Path.of(strRemoteBase); //= Path.of("/pub/databases/opentargets/platform/21.11/output/etl/json/");
        Path localDirBase = Path.of(strLocalBase); //= Path.of(".");
        Path dir = Path.of(strDir); //= Path.of("evidence/sourceId=eva/");

        Path remoteDir = remoteDirBase.resolve(dir);
        Path localDir = localDirBase.resolve(dir);

        System.out.println();
        System.out.println("Remote FTP server: [" + server + "]");
        System.out.println("Remote source directory: [" +  remoteDir.toString() +"]");
        System.out.println("Local destination directory: [" + localDir.toString() + "]");

        // ensure we have the local dir structure in place
        try {
            Files.createDirectories(localDir);
        } catch (IOException ex) {
            System.out.format("Unable to create local directory: [%s] reason: [%s]"
                    , localDir.toString(), ex.getMessage());
            System.out.println();
            System.exit(0);
        }

        // download file from FTP server
        try(var client = FtpClient.of(server)) {
            var files = client.listFiles(remoteDir);

            // have the same predicate for counting and downloading
            Predicate<FTPFile> fileFilter = FTPFile::isFile;

            //  find total number of files to download
            totalFilesToDownload = files.stream()
                    .filter(fileFilter)
                    .count();

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

                        client.downloadFile(remoteDir, Path.of(f.getName()), localDir);
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
