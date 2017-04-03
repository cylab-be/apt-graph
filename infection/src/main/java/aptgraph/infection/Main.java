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
package aptgraph.infection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    /**
     * @param args the command line arguments
     * @throws ParseException If we cannot parse command line args
     * @throws FileNotFoundException If the input file does not exist
     * @throws IOException If we cannot read the input file
     * @throws IllegalArgumentException If wrong arguments are given
     * @throws java.text.ParseException If text can't be parsed
     */
    public static void main(final String[] args)
        throws ParseException, FileNotFoundException, IOException,
            IllegalArgumentException, java.text.ParseException {
        // Default value of arguments
        String format = DEFAULT_FORMAT;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input file (required)");
        options.addOption("o", true, "Output file (required)");
        options.addOption("d", true, "APT domain (required)");
        options.addOption("t", true, "Type (required)");
        options.addOption("u", true, "User (required)");
        Option arg_time_step = Option.builder("step")
                .optionalArg(true)
                .desc("Specify time step between periodic"
                        + " injection [min] (required for periodic APT)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_time_step);
        Option arg_delta_time = Option.builder("delta")
                .optionalArg(true)
                .desc("Duration between two requests of the same burst"
                        + " (required for traffic APT)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_delta_time);
        Option arg_duration = Option.builder("duration")
                .optionalArg(true)
                .desc("Duration of a burst to allow APT injection"
                        + " (required for traffic APT)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_duration);
        Option arg_injection_day = Option.builder("injection")
                .optionalArg(true)
                .desc("Daily number of injection (required for traffic APT)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_injection_day);
        Option arg_proportion = Option.builder("proportion")
                .optionalArg(true)
                .desc("Injection rate in the bursts (1 = inject in all "
                        + "the burst) (required for traffic APT)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_proportion);
        Option arg_format = Option.builder("f")
                .optionalArg(true)
                .desc("Specify format of input file (default : squid)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_format);
        options.addOption("h", false, "Show this help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")
                || !cmd.hasOption("o")
                || !cmd.hasOption("d")
                || !cmd.hasOption("t")
                || !cmd.hasOption("u")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar infection-<version>.jar", options);
            return;
        }

        String type = "";
        try {
            type = cmd.getOptionValue("t");
            if (!type.equals("periodic") && !type.equals("traffic")) {
                throw new IllegalArgumentException("Wrong type option");
            }
        } catch (IllegalArgumentException ex) {
            System.err.println(ex);
        }

        long time_step = 0L;
        long delta_time = Long.MAX_VALUE;
        long duration = 0;
        int injection_day = 0;
        double proportion = 1.0;
        try {
            if (type.equals("periodic")) {
                if (!cmd.hasOption("step")) {
                    throw new IllegalArgumentException(
                            "Time Step is not given");
                } else {
                    time_step = Long.parseLong(cmd.getOptionValue("step"));
                }
            } else if (type.equals("traffic")) {
                if (!cmd.hasOption("delta")) {
                    throw new IllegalArgumentException(
                            "Delta Time is not given");
                } else {
                    delta_time = Long.parseLong(cmd.getOptionValue("delta"));
                }
                if (!cmd.hasOption("duration")) {
                    throw new IllegalArgumentException(
                            "Duration is not given");
                } else {
                    duration = Long.parseLong(cmd.getOptionValue("duration"));
                }
                if (!cmd.hasOption("injection")) {
                    throw new IllegalArgumentException(
                            "Injection by Day is not given");
                } else {
                    injection_day = Integer.parseInt(
                            cmd.getOptionValue("injection"));
                }
                if (!cmd.hasOption("proportion")) {
                    throw new IllegalArgumentException(
                            "Proportion is not given");
                } else {
                    proportion = Double.parseDouble(
                            cmd.getOptionValue("proportion"));
                }
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

        FileOutputStream output_stream =
                new FileOutputStream(cmd.getOptionValue("o"));

        try {
            ApplyAPT apt = new ApplyAPT();
            if (type.equals("periodic")) {
                apt.periodicAPT(
                        new FileInputStream(cmd.getOptionValue("i")),
                        output_stream,
                        cmd.getOptionValue("d"),
                        cmd.getOptionValue("u"),
                        format,
                        time_step);
            } else if (type.equals("traffic")) {
                apt.trafficAPT(
                        new FileInputStream(cmd.getOptionValue("i")),
                        output_stream,
                        cmd.getOptionValue("d"),
                        cmd.getOptionValue("u"),
                        format,
                        delta_time,
                        duration,
                        injection_day,
                        proportion);
            }
        } finally {
            output_stream.close();
        }

    }

    private Main() {
    }

}
