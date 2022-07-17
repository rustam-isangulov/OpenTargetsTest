package FtpUtil.cli;

import org.apache.commons.cli.*;

import java.net.URI;
import java.nio.file.Path;

public class CommandLineParameters {
    private URI server;
    private Path remoteBase;
    private Path localBase;
    private Path dir;

    private Options options;

    public URI getServer() {
        return server;
    }

    public Path getRemoteBase() {
        return remoteBase;
    }

    public Path getLocalBase() {
        return localBase;
    }

    public Path getDir() {
        return dir;
    }

    public void parse(String... args) throws ParseException {

        // define options

        options = new Options();

        Option serverURI = Option.builder()
                .option("s")
                .longOpt("server")
                .argName("ftp_address")
                .required()
                .hasArg()
                .desc("remote ftp server uri")
                .build();

        Option remoteBaseOp = Option.builder()
                .option("r")
                .longOpt("remotedir")
                .argName("remote_dir")
                .required()
                .hasArg()
                .desc("remote base directory")
                .build();

        Option localBaseOp = Option.builder()
                .option("l")
                .longOpt("localdir")
                .argName("local_dir")
                .required()
                .hasArg()
                .desc("local base directory")
                .build();

        Option dirOp = Option.builder()
                .option("d")
                .longOpt("dir")
                .argName("dir")
                .required()
                .hasArg()
                .desc("directory to download files from (relative to remotedir) and to (relative to localdir)")
                .build();

        options.addOption(serverURI);
        options.addOption(remoteBaseOp);
        options.addOption(localBaseOp);
        options.addOption(dirOp);

        // parse the command line

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        // parse server
        server = URI.create(line.getOptionValue(serverURI));

        // parse base directory
        remoteBase = Path.of(line.getOptionValue(remoteBaseOp));

        // parse base directory
        localBase = Path.of(line.getOptionValue(localBaseOp));

        // parse target directory
        dir = Path.of(line.getOptionValue(dirOp));

        //throw new ParseException("test");
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        String footer = "\nExample:\n java -jar ftputil.jar" +
                " -s \"ftp.ebi.ac.uk\"" +
                " -r \"/pub/databases/opentargets/platform/21.11/output/etl/json/\"" +
                " -l \"./data/\"" +
                " -d \"evidence/sourceId=eva/\"";
        formatter.printHelp
                ("java -jar ftputil.jar"
                        , "\nDownload files from a directory on an ftp server"
                                + "\n\nOptions:" , options, footer, true);
    }

}

















