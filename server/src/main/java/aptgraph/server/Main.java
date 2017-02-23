package aptgraph.server;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Thibault Debatty
 */
public final class Main {

    /**
     *
     * @param args
     * @throws org.apache.commons.cli.ParseException if command line cannot be
     * parsed
     * @throws java.io.FileNotFoundException if the graph file is not found
     * @throws java.lang.ClassNotFoundException if the classes corresponding to
     * graph elements are not found
     * @throws java.lang.Exception if the server cannot start
     */
    public static void main(final String[] args)
            throws ParseException, FileNotFoundException,
            ClassNotFoundException, Exception {

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input file (required)");
        options.addOption("h", false, "Show this help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h") || !cmd.hasOption("i")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar server-<version>.jar", options);
            return;
        }

        // Start the json-rpc server
        System.out.println("Input File : " + cmd.getOptionValue("i"));
        JsonRpcServer jsonrpc_server = new JsonRpcServer(
                new FileInputStream(cmd.getOptionValue("i")));
        jsonrpc_server.startInBackground();

        String url = "http://127.0.0.1:8000";

        if (Desktop.isDesktopSupported()) {
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
        }

        // Start the file server
        FileServer file_server = new FileServer();
        file_server.start();
    }

    private Main() {
    }
}
