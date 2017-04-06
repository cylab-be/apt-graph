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
package aptgraph.config;

import aptgraph.server.JsonRpcServer;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Thomas Gilon
 */
public final class Main {

    private static final Logger LOGGER
            = Logger.getLogger(JsonRpcServer.class.getName());

    /**
     * @param args the command line arguments
     * @throws org.apache.commons.cli.ParseException If text can't be parsed
     */
    public static void main(final String[] args) throws ParseException {

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input config file (required)");
        options.addOption("o", true, "Output config file (required)");
        options.addOption("field", true, "Config field to sweep (required)");
        options.addOption("start", true, "Start value of sweep (required)");
        options.addOption("stop", true, "Stop value of sweep (required)");
        options.addOption("step", true, "Step value of sweep (required)");
        Option arg_multi = Option.builder("multi")
                .optionalArg(true)
                .desc("Sweep the given field in complement to 1 "
                        + "of the first one")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_multi);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")
                || !cmd.hasOption("o")
                || !cmd.hasOption("field")
                || !cmd.hasOption("start")
                || !cmd.hasOption("stop")
                || !cmd.hasOption("step")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar infection-<version>.jar", options);
            return;
        }

        JSONObject obj;
        List<String> config = null;
        try {
            config = Files.readAllLines(
                    Paths.get(cmd.getOptionValue("i")),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        String field = "";
        BigDecimal start = BigDecimal.ZERO;
        BigDecimal stop = BigDecimal.ZERO;
        BigDecimal step = BigDecimal.ZERO;
        try {
            field = cmd.getOptionValue("field");
            start = new BigDecimal(cmd.getOptionValue("start"));
            stop = new BigDecimal(cmd.getOptionValue("stop"));
            step = new BigDecimal(cmd.getOptionValue("step"));
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }
        String multi = "";
        try {
            if (cmd.hasOption("multi")) {
                multi = cmd.getOptionValue("multi");
            }
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
        }

        boolean first_write = true;
        for (String config_line : config) {
            try {
                obj = new JSONObject(config_line);
            } catch (JSONException ex) {
                throw new JSONException(ex + "\nJSON did not match ");
            }
            LinkedList<String> config_lines_sweeped =
                    Sweep.sweepObj(obj, field, start, stop, step, multi);
            try {
                if (!(new File(cmd.getOptionValue("o"))).exists()
                    || first_write) {
                    Files.write(Paths.get(cmd.getOptionValue("o")),
                            config_lines_sweeped);
                    first_write = false;
                } else {
                    Files.write(Paths.get(cmd.getOptionValue("o")),
                            config_lines_sweeped, StandardOpenOption.APPEND);
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        LOGGER.log(Level.INFO, "Config file saved to {0}",
                cmd.getOptionValue("o"));
    }

    private Main() {
    }
}
