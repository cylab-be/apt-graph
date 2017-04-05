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

import aptgraph.core.Domain;
import aptgraph.server.RequestHandler;
import aptgraph.server.Output;
import java.io.IOException;
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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Thomas Gilon
 */
public final class Main {

    /**
     * @param args the command line arguments
     * @throws org.apache.commons.cli.ParseException If text can't be parsed
     */
    public static void main(final String[] args) throws ParseException {
        // Parse command line arguments
        Options options = new Options();
        options.addOption("i", true, "Input config file (required)");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")
                || !cmd.hasOption("i")) {

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar infection-<version>.jar", options);
            return;
        }

        JSONObject obj;
        List<String> config = null;
        try {
            config = Files.readAllLines(
                    Paths.get(cmd.getOptionValue("i")));
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

            Output output = handler.analyze(obj.getString("user"),
                    listToDouble((List<Double>) (Object)
                            obj.getJSONArray("feature_weights").toList()),
                    listToDouble((List<Double>) (Object)
                          obj.getJSONArray("feature_ordered_weights").toList()),
                    obj.getDouble("prune_threshold"),
                    obj.getDouble("max_cluster_size"),
                    obj.getBoolean("prune_z"),
                    obj.getBoolean("cluster_z"),
                    obj.getBoolean("whitelist"),
                    obj.getString("white_ongo"),
                    obj.getInt("number_requests"),
                    listToDouble((List<Double>) (Object)
                            obj.getJSONArray("ranking_weights").toList()),
                    obj.getBoolean("apt_search"));

            TreeMap<Double, LinkedList<Domain>> ranking = output.getRanking();

            int n_apt_tot = obj.getInt("n_apt_tot");
            ROC.makeROC(ranking, handler.getMemory().getAllDomains()
                    .get("all").values().size() - n_apt_tot, n_apt_tot,
                    obj.getString("output_file"));
        }
    }

    /**
     * Convert List to double[].
     * @param list
     * @return array
     */
    private static double[] listToDouble(final List<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private Main() {
    }
}
