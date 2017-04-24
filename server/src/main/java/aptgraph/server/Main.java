package aptgraph.server;

//import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
//import java.io.IOException;
//import java.net.URI;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main class for Server.
 *
 * @author Thibault Debatty
 */
public final class Main {

    private static final boolean DEFAULT_STUDY_OUT = false;

    /**
     * Main method of Server.
     *
     * @param args Arguments from the command line
     * @throws org.apache.commons.cli.ParseException If command line cannot be
     * parsed
     * @throws java.io.FileNotFoundException If the graph file is not found
     * @throws java.lang.ClassNotFoundException If the classes corresponding to
     * graph elements are not found
     * @throws java.lang.Exception If the server cannot start
     */
    public static void main(final String[] args)
            throws ParseException, FileNotFoundException,
            ClassNotFoundException, Exception {
        // Default value of arguments
        boolean study_out = DEFAULT_STUDY_OUT;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input directory with graphs (required)");
        options.addOption("h", false, "Show this help");
        Option arg_study = Option.builder("study")
                .optionalArg(true)
                .desc("Study output mode (false = web output, true ="
                        + " study output) (option, default: false)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_study);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h") || !cmd.hasOption("i")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar server-<version>.jar", options);
            return;
        }

        try {
            if (cmd.hasOption("study")) {
                study_out = Boolean.parseBoolean(cmd.getOptionValue("study"));
            }
        } catch (IllegalArgumentException ex) {
            System.err.println(ex);
        }

        // Start the json-rpc server
        JsonRpcServer jsonrpc_server = new JsonRpcServer(
                Paths.get(cmd.getOptionValue("i")), study_out);
        jsonrpc_server.startInBackground();

        String url = "http://127.0.0.1:8000";

        /*if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }*/

        // Start the file server
        FileServer file_server = new FileServer();
        file_server.start();
    }

    private Main() {
    }
}
