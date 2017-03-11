package aptgraph.batch;

import aptgraph.core.Request;
import aptgraph.core.TimeSimilarity;
//import aptgraph.core.URLSimilarity;
import aptgraph.core.DomainSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import info.debatty.java.graphs.build.Brute;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
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
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessor {

    private static final Logger LOGGER = Logger.getLogger(
            BatchProcessor.class.getName());

    // Regex to use for the full match of the squid log
    private static final String REGEX
            = "^(\\d+\\.\\d+)\\s+(\\d+)\\s"
            + "([^\\s]+)\\s"
            + "(\\S+)\\/(\\d+)\\s(\\d+)\\s(\\S+)\\s(\\S+)\\s\\-\\s(\\S+)\\/"
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
     * @param k
     * @param input_file
     * @param output_dir
     * @throws IOException if we cannot read the input file
     */
    public final void analyze(final int k,
            final InputStream input_file, final Path output_dir)
            throws IOException {

        // Parsing of the log file and Split of the log file by users
        LOGGER.info("Read and parse input file...");
        HashMap<String, LinkedList<Request>> user_requests =
                computeUserLog(parseFile(input_file));

        // Build graphs for each user
        ArrayList<String> user_list = new ArrayList<String>();
        for (Map.Entry<String, LinkedList<Request>> entry
                : user_requests.entrySet()) {
            String user = entry.getKey();
            LinkedList<Request> requests = entry.getValue();

            LinkedList<Graph<Request>> graphs =
                    computeUserGraphs(k, user, requests);

            // Store of the list of graphs for one user on disk
            saveGraphs(output_dir, user, graphs);
            user_list.add(user);
        }
        saveUsers(output_dir, user_list);
    }

    /**
     * Read and parse the input file line by line.
     * @param file
     * @return LinkedList<Request>
     */
    final LinkedList<Request> parseFile(final InputStream file)
            throws IOException {

        LinkedList<Request> requests = new LinkedList<Request>();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(file, "UTF-8"));
        String line = null;

        while ((line = in.readLine()) != null) {
            try {
                requests.add(parseLine(line));

            } catch (IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
            }
        }

        return requests;
    }

    /**
     * Parse a give line.
     * @param line
     * @return  Request
     */
    private Request parseLine(final String line) {

        Matcher match = pattern.matcher(line);

        if (!match.matches()) {
            throw new IllegalArgumentException("Regex did not match " + line);
        }

        String thisdomain = null;

        try {
            thisdomain = computeDomain(match.group(8));
        } catch (MalformedURLException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        Request request = new Request(
                (long) (Double.parseDouble(match.group(1)) * 1000),
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
    final HashMap<String, LinkedList<Request>> computeUserLog(
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
     * @throws MalformedURLException if url is not correctly formed
     */
    private static String computeDomain(final String url)
            throws MalformedURLException {
        String url_temp = url;
        if (url_temp.startsWith("tcp://")) {
            String[] url_split = url_temp.split("[:]//");
            url_temp = url_split[1];
        }
        if (!url_temp.startsWith("http://")
                && !url_temp.startsWith("https://")) {
         url_temp = "http://" + url_temp;
        }
        if (url_temp.contains(":")) {
            String[] url_split = url_temp.split("[:]");
            url_temp = url_split[0] + ":" + url_split[1];
        }
        String domain = "";
        try {
            URL myurl = new URL(url_temp);
            domain = myurl.getHost();
        } catch (MalformedURLException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                  .log(Level.SEVERE, "URL " + url + " is a malformed URL", ex);
        }
        if (domain.startsWith("www.")) {
            return domain.substring(4);
        }
        return domain;
    }

    /**
     * Compute the feature graphs of a user.
     * @param k
     * @param user
     * @param requests
     * @return LinkedList<Graph<Request>>
     * @throws IOException
     */
    final LinkedList<Graph<Request>> computeUserGraphs(
            final int k,
            final String user,
            final LinkedList<Request> requests) {

        LOGGER.log(Level.INFO,
                "Build the time based graph for user {0} ...", user);
        Graph<Request> time_graph;
        if (requests.size() < 50) {
            Brute<Request> nndes_time = new Brute<Request>();
            nndes_time.setSimilarity(new TimeSimilarity());
            nndes_time.setK(k);
            time_graph = nndes_time.computeGraph(requests);
        } else {
            ThreadedNNDescent<Request> nndes_time =
                    new ThreadedNNDescent<Request>();
            nndes_time.setSimilarity(new TimeSimilarity());
            nndes_time.setK(k);
            time_graph = nndes_time.computeGraph(requests);
        }

        /*LOGGER.log(Level.INFO,
                "Build the URL based graph for user {0} ...", user);
        Graph<Request> url_graph;
        if (requests.size() < 50) {
            Brute<Request> nndes_url = new Brute<Request>();
            nndes_url.setSimilarity(new URLSimilarity());
            nndes_url.setK(k);
            url_graph = nndes_url.computeGraph(requests);
        } else {
            ThreadedNNDescent<Request> nndes_url =
                    new ThreadedNNDescent<Request>();
            nndes_url.setSimilarity(new URLSimilarity());
            nndes_url.setK(k);
            url_graph = nndes_url.computeGraph(requests);
        }*/

        LOGGER.log(Level.INFO,
                "Build the Domain based graph for user {0} ...", user);
        Graph<Request> domain_graph;
        if (requests.size() < 50) {
            Brute<Request> nndes_domain = new Brute<Request>();
            nndes_domain.setSimilarity(new DomainSimilarity());
            nndes_domain.setK(k);
            domain_graph = nndes_domain.computeGraph(requests);
        } else {
            ThreadedNNDescent<Request> nndes_domain =
                    new ThreadedNNDescent<Request>();
            nndes_domain.setSimilarity(new DomainSimilarity());
            nndes_domain.setK(k);
            domain_graph = nndes_domain.computeGraph(requests);
        }

        // List of graphs
        LinkedList<Graph<Request>> graphs =
                new LinkedList<Graph<Request>>();
        graphs.add(time_graph);
        //graphs.add(url_graph);
        graphs.add(domain_graph);

        return graphs;
    }

    /**
     * Save the feature graphs of a user.
     * @param output_dir
     * @param user
     * @param graphs
     * @throws IOException
     */
    final void saveGraphs(
        final Path output_dir,
        final String user,
        final LinkedList<Graph<Request>> graphs) {

        try {
            LOGGER.log(Level.INFO,
                    "Save graphs of user {0} to disk...", user);
            File file = new File(output_dir.toString(), user + ".ser");
            if (Files.notExists(output_dir)) {
                Files.createDirectory(output_dir);
            }
            FileOutputStream output_stream =
                new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            output.writeObject(graphs);
            output.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }
    }

    /**
     * Save the list of the users.
     * @param output_dir
     * @param user_list
     */
    final void saveUsers(
            final Path output_dir,
            final ArrayList<String>  user_list) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of users to disk...");
            File file = new File(output_dir.toString(), "users.ser");
            FileOutputStream output_stream =
                new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            Collections.sort(user_list);
            output.writeObject(user_list);
            output.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }
    }
}
