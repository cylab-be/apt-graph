package aptgraph.batch;

import aptgraph.core.Request;
import aptgraph.core.TimeSimilarity;
import aptgraph.core.URLSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessor {

    private static final Logger LOGGER = Logger.getLogger(
            BatchProcessor.class.getName());

    // Regex to use for the full match of the squid log
    private static final String REGEX
            = "^(\\d+\\.\\d+)\\s*(\\d+)\\s"
            + "([^\\s]+)\\s"
            + "(\\S+)\\/(\\d{3})\\s(\\d+)\\s(\\S+)\\s(\\S+)\\s\\-\\s(\\S+)\\/"
            + "([^\\s]+)\\s(\\S+).*$";

    private final Pattern pattern;

    /**
     *
     */
    public BatchProcessor() {
        pattern = Pattern.compile(REGEX);
    }

    /**
     *
     * @param input_file
     * @param output_file
     * @throws IOException if we cannot read the input file
     */
    public final void analyze(
            final InputStream input_file, final FileOutputStream output_file)
            throws IOException {

        // Choice of the k of the k-NN Graph
        int myk = 3;

        // Parsing of the log file
        LOGGER.info("Read and parse input file...");

        // Split of the log file by users
        LinkedList<Request> requests_temp = parseFile(input_file);
        HashMap<String, LinkedList<Request>> user_requests =
                computeUserLog(requests_temp);

        // Build graphs for each user
        HashMap<String, LinkedList<Graph>> user_graphs =
                new HashMap<String, LinkedList<Graph>>();
        for (HashMap.Entry<String, LinkedList<Request>> entry
                : user_requests.entrySet()) {
            String user = entry.getKey();
            LinkedList<Request> requests = entry.getValue();

            LOGGER.log(Level.INFO,
                    "Build the time based graph for user {0} ...", user);
            ThreadedNNDescent<Request> nndes_time =
                    new ThreadedNNDescent<Request>();
            nndes_time.setSimilarity(new TimeSimilarity());
            nndes_time.setK(myk);
            Graph<Request> time_graph = nndes_time.computeGraph(requests);

            LOGGER.log(Level.INFO,
                    "Build the URL based graph for user {0} ...", user);
            ThreadedNNDescent<Request> nndes_url =
                    new ThreadedNNDescent<Request>();
            nndes_url.setSimilarity(new URLSimilarity());
            nndes_url.setK(myk);
            Graph<Request> url_graph = nndes_url.computeGraph(requests);

            // List of graphs
            LinkedList<Graph> graphs = new LinkedList<Graph>();
            graphs.add(time_graph);
            graphs.add(url_graph);

            // Store of the list of graphs for one user
            user_graphs.put(user, graphs);
        }

        System.out.println(user_graphs);

        LOGGER.info("Save graphs to disk...");
        ObjectOutputStream output_time = new ObjectOutputStream(
                new BufferedOutputStream(output_file));
        output_time.writeObject(user_graphs);
        output_time.close();

    }

    /**
     * Read and parse the input file line by line.
     *
     * @param file
     * @return
     */
    private LinkedList<Request> parseFile(final InputStream file)
            throws IOException {

        LinkedList<Request> requests = new LinkedList<Request>();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(file, "UTF-8"));
        String line = null;

        while ((line = in.readLine()) != null) {
            requests.add(parseLine(line));
        }

        return requests;
    }

    private Request parseLine(final String line) {

        Matcher match = pattern.matcher(line);

        if (!match.matches()) {
            throw new IllegalArgumentException("Regex did not match " + line);
        }

        String thisdomain = null;

        try {
            thisdomain = computeDomain(match.group(8));

        } catch (URISyntaxException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        Request request = new Request(
                Double.parseDouble(match.group(1)),
                Integer.parseInt(match.group(2)),
                match.group(3),
                match.group(4),
                Integer.parseInt(match.group(5)),
                Integer.parseInt(match.group(6)),
                match.group(7),
                match.group(8),
                thisdomain,
                match.group(9),
                match.group(10),
                match.group(11));

        return request;
    }

    /** Associate each user (String) to his requests (LinkedList<Request>).
     * @param requests_temp : LinkedList<Request> of the raw log file
     * @return user_requests : HashMap<String, LinkedList<Request>
     * of the log file sorted by user
     */
    private HashMap<String, LinkedList<Request>> computeUserLog(
        final LinkedList<Request> requests_temp) {
        HashMap<String, LinkedList<Request>> user_requests =
                new HashMap<String, LinkedList<Request>>();
        for (Request req : requests_temp) {
            String user = req.getClient();

            LinkedList<Request> requests;
            if (user_requests.containsKey(user)) {
                requests = user_requests.get(user);

            } else {
                requests = new LinkedList<Request>();
                user_requests.put(user, requests);
            }

            requests.add(req);

        }
        return user_requests;
    }


    /**
     * Return the domain name from URL (without wwww.).
     * @param url
     * @return
     * @throws URISyntaxException if url is not correctly formed
     */
    private static String computeDomain(final String url)
            throws URISyntaxException {
        String url_temp = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")
                && url.endsWith(":443")) {
         url_temp = "https://" + url;
        }
        URI uri = new URI(url_temp);
        String domain = uri.getHost();
        if (domain.startsWith("www.")) {
            return domain.substring(4);
        }

        return domain;
    }

}
