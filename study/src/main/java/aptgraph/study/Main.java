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
package aptgraph.study;

import aptgraph.server.RequestHandler;
import aptgraph.server.Output;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
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
 * Main class for Study.
 *
 * @author Thomas Gilon
 */
public final class Main {

    private static final boolean DEFAULT_OVERWRITE_BOOL = false;
    private static final Logger LOGGER = Logger.getLogger(
            Main.class.getName());

    /**
     * Main method of Study.
     *
     * @param args Arguments from the command line
     * @throws org.apache.commons.cli.ParseException If text can't be parsed
     */
    public static void main(final String[] args) throws ParseException {
        // Default value of arguments
        boolean overwrite_bool = DEFAULT_OVERWRITE_BOOL;

        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input configuration file (required)");
        Option arg_overwrite = Option.builder("x")
                .optionalArg(true)
                .desc("Overwrite existing files (option, default : false)")
                .hasArg(true)
                .numberOfArgs(1)
                .build();
        options.addOption(arg_overwrite);
        options.addOption("h", false, "Show this help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar study-<version>.jar", options);
            return;
        }
        try {
            if (cmd.hasOption("x")) {
                overwrite_bool = Boolean.parseBoolean(cmd.getOptionValue("x"));
            }
        } catch (IllegalArgumentException ex) {
                System.err.println(ex);
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

        String input_dir_store = "";
        RequestHandler handler = null;
        for (String config_line : config) {
            try {
                obj = new JSONObject(config_line);
            } catch (JSONException ex) {
                throw new JSONException(ex + "\nJSON did not match ");
            }
            if (!input_dir_store.equals(obj.getString("input_dir"))) {
                input_dir_store = obj.getString("input_dir");
                handler = new RequestHandler(
                    Paths.get(input_dir_store));
            }

            File file = new File(obj.getString("output_file"));
            if (overwrite_bool || !file.exists()) {
                Output output = handler.analyze(obj.getString("user"),
                        new double[]{obj.getDouble("feature_weights_time"),
                        obj.getDouble("feature_weights_domain"),
                        obj.getDouble("feature_weights_url")},
                        new double[]{obj.getDouble("feature_ordered_weights_1"),
                        obj.getDouble("feature_ordered_weights_2")},
                        obj.getDouble("prune_threshold"),
                        obj.getDouble("max_cluster_size"),
                        obj.getBoolean("prune_z"),
                        obj.getBoolean("cluster_z"),
                        obj.getBoolean("whitelist"),
                        obj.getString("white_ongo"),
                        obj.getInt("number_requests"),
                        new double[]{obj.getDouble("ranking_weights_parents"),
                        obj.getDouble("ranking_weights_children"),
                        obj.getDouble("ranking_weights_requests")},
                        obj.getBoolean("apt_search"));

                TreeMap<Double, LinkedList<String>> ranking
                        = output.getRanking();

                // Show APT founded
                String[] apt_infos = output.getStdout().split("<br>");
                boolean apt_print = false;
                for (int i = 0; i < apt_infos.length; i++) {
                    if (apt_infos[i].startsWith("Number of APT domains")) {
                        apt_print = true;
                    }
                    if (apt_infos[i].startsWith("Ranking")) {
                        break;
                    }
                    if (apt_print) {
                        System.out.println(apt_infos[i]);
                    }
                }

                int n_apt_tot = obj.getInt("n_apt_tot");
                ROC.makeROC(ranking, handler.getMemory().getAllDomains()
                        .get("all").values().size() - n_apt_tot, n_apt_tot,
                        obj.getString("output_file"));
            } else {
                LOGGER.log(Level.INFO,
                "File {0} has been skipped...", obj.getString("output_file"));
            }
        }
    }

    private Main() {
    }
}
