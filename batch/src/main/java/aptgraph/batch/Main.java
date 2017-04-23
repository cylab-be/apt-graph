/*
 * The MIT License
 *
 * Copyright 2017 Thibault Debatty & Thomas Gilon.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
 * Main class for Batch Processor.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
 */
public final class Main {

    private static final int DEFAULT_K = 20;
    private static final boolean DEFAULT_CHILDREN_BOOL = true;
    private static final boolean DEFAULT_OVERWRITE_BOOL = false;
    private static final String DEFAULT_FORMAT = "squid";

    /**
     * Main method of Batch Processor.
     *
     * @param args Arguments from the command line
     * @throws ParseException If we cannot parse command line args
     * @throws FileNotFoundException If the input file does not exist
     * @throws IOException If we cannot read the input file
     * @throws IllegalArgumentException If argument k is not an int
     */
    public static void main(final String[] args)
            throws ParseException, FileNotFoundException, IOException,
            IllegalArgumentException {
        // Default value of arguments
        int k = DEFAULT_K;
        boolean children_bool = DEFAULT_CHILDREN_BOOL;
        boolean overwrite_bool = DEFAULT_OVERWRITE_BOOL;
        String format = DEFAULT_FORMAT;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input log file (required)");
        options.addOption("o", true, "Output directory for graphs (required)");
        Option arg_k = Option.builder("k")
                .optionalArg(true)
                .desc("Impose k value of k-NN graphs (option, default: 20)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_k);
        Option arg_child = Option.builder("c")
                .optionalArg(true)
                .desc("Select only temporal children (option, default: true)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_child);
        Option arg_overwrite = Option.builder("x")
                .optionalArg(true)
                .desc("Overwrite existing graphs (option, default : false)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_overwrite);
        Option arg_format = Option.builder("f")
                .optionalArg(true)
                .desc("Specify format of input file (squid or json) "
                        + "(option, default : squid)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_format);
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
            if (cmd.hasOption("f")) {
                format = cmd.getOptionValue("f");
                if (!format.equals("squid") && !format.equals("json")) {
                    throw new IllegalArgumentException("Wrong format option");
                }
            }
        } catch (IllegalArgumentException ex) {
            System.err.println(ex);
        }

        // Run analyze
        try {
            BatchProcessor processor = new BatchProcessor();
            processor.analyze(k,
                    new FileInputStream(cmd.getOptionValue("i")),
                    Paths.get(cmd.getOptionValue("o")),
                    format, children_bool, overwrite_bool);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex);
        }

    }

    private Main() {
    }
}
