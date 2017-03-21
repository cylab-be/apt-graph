package aptgraph.batch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Thibault Debatty
 */
public final class Main {

    private static final int DEFAULT_K = 20;

    /**
     *
     * @param args
     * @throws ParseException If we cannot parse command line args
     * @throws FileNotFoundException if the input file does not exist
     * @throws IOException if we cannot read the input file
     * @throws IllegalArgumentException if argument k is not an int
     */
    public static void main(final String[] args)
            throws ParseException, FileNotFoundException, IOException,
            IllegalArgumentException {
        // Default value of k (if no user input)
        int k = DEFAULT_K;
        // Default value of arguments
        boolean children_bool = true;
        boolean overwrite_bool = false;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input file (required)");
        options.addOption("o", true, "Output directory (required)");
        Option arg_k = Option.builder("k")
                .optionalArg(true)
                .desc("Impose k value of k-NN graphs (default: 20)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_k);
        Option arg_child = Option.builder("c")
                .optionalArg(true)
                .desc("Select only temporal children (default: true)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_child);
        Option arg_overwrite = Option.builder("x")
                .optionalArg(true)
                .desc("Overwrite existing graphs (default : false)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_overwrite);
        options.addOption("h", false, "Show this help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")
                || !cmd.hasOption("o")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar batch-<version>.jar", options);
            return;
        }

        try {
            if (cmd.hasOption("k")) {
                k = Integer.parseInt(cmd.getOptionValue("k"));
            }
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }

        try {
            if (cmd.hasOption("c")) {
                children_bool = Boolean.parseBoolean(cmd.getOptionValue("c"));
            }
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }

        try {
            if (cmd.hasOption("x")) {
                overwrite_bool = Boolean.parseBoolean(cmd.getOptionValue("x"));
            }
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }

        try {
            BatchProcessor processor = new BatchProcessor();
            processor.analyze(k,
                    new FileInputStream(cmd.getOptionValue("i")),
                    Paths.get(cmd.getOptionValue("o")),
                    children_bool, overwrite_bool);
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }

    }

    private Main() {
    }
}
