package aptgraph.batch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
     * @throws ParseException If we cannot parse command line args
     * @throws FileNotFoundException if the input file does not exist
     * @throws IOException if we cannot read the input file
     */
    public static void main(final String[] args)
            throws ParseException, FileNotFoundException, IOException {

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input file (required)");
        options.addOption("o", true, "Output file (required)");
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

        BatchProcessor processor = new BatchProcessor();
        processor.analyze(
                new FileInputStream(cmd.getOptionValue("i")),
                new FileOutputStream(cmd.getOptionValue("o")));

    }

    private Main() {
    }
}
