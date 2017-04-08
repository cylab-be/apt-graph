/*
 * The MIT License
 *
 * Copyright 2017 Thomas Gilon.
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
package aptgraph.traffic;

import aptgraph.batch.BatchProcessor;
import aptgraph.core.Request;
import aptgraph.server.HistData;
import aptgraph.server.Utility;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Thomas Gilon
 */
public final class Main {

    private static final String DEFAULT_FORMAT = "squid";
    private static final Logger LOGGER
            = Logger.getLogger(Main.class.getName());

    /**
     * @param args the command line arguments
     * @throws org.apache.commons.cli.ParseException If text can't be parsed
     */
    public static void main(final String[] args)
            throws ParseException {
        // Default value of arguments
        String format = DEFAULT_FORMAT;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input log file (required)");
        options.addOption("o", true, "Output CSV file (required)");
        options.addOption("r", true, "Resolution (required)");
        Option arg_format = Option.builder("f")
                .optionalArg(true)
                .desc("Specify format of input file (default : squid)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_format);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")
                || !cmd.hasOption("o")
                || !cmd.hasOption("r")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar traffic-<version>.jar", options);
            return;
        }

        double res = 0.0;
        try {
            res = Double.parseDouble(cmd.getOptionValue("r"));
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

        InputStream file = null;
        try {
            file = new FileInputStream(cmd.getOptionValue("i"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Compute Histogram
        BatchProcessor processor = new BatchProcessor();
        LinkedList<Request> requests = processor.parseFile(file, format);
        LOGGER.log(Level.INFO, "File Parsed ({0} requests)", requests.size());
        long start_time = requests.getFirst().getTime();
        long stop_time = requests.getLast().getTime();
        ArrayList<Double> list = new ArrayList<Double>();
        for (int i = 0; i < requests.size(); i++) {
            list.add(i, (double) requests.get(i).getTime());
        }
        LOGGER.log(Level.INFO, "List generated");
        HistData histogram = Utility.computeHistogram(list,
                (double) start_time, (double) stop_time, res);

        // Export to CSV file
        OutputStream output_file = null;
        try {
            output_file = new FileOutputStream(cmd.getOptionValue("o"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            for (Entry<Double, Double> entry : histogram.entrySet()) {
                output_file.write((entry.getKey() + ","
                        + entry.getValue() + "\n").getBytes("UTF-8"));
            }
            output_file.close();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Main.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
                Logger.getLogger(Main.class.getName())
                        .log(Level.SEVERE, null, ex);
        }

        LOGGER.log(Level.INFO, "Traffic file saved to {0}",
                cmd.getOptionValue("o"));
    }

    private Main() {
    }
}
