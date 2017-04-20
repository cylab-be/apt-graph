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

import aptgraph.core.Request;
import aptgraph.core.Domain;
import aptgraph.core.Subnet;
import aptgraph.core.TimeSimilarity;
//import aptgraph.core.URLSimilarity;
import aptgraph.core.DomainSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import info.debatty.java.graphs.SimilarityInterface;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import info.debatty.java.graphs.build.Brute;
import info.debatty.java.graphs.build.NNDescent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Batch Processor definition file.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
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
     * Produce domain graphs for each user and each similarities based on a
     * given log and set of parameters.
     *
     * @param k k value of k-NN Graphs
     * @param input_file Input file
     * @param output_dir Output directory
     * @param format File format (JSON or SQUID)
     * @param children_bool Boolean for children selection
     * @param overwrite_bool Boolean allowing overwrite of existing graphs
     * @throws IOException If we cannot read the input file
     */
    public final void analyze(final int k,
            final InputStream input_file, final Path output_dir,
            final String format,
            final boolean children_bool,
            final boolean overwrite_bool)
            throws IOException {

        // Parsing of the log file and Split of the log file by users
        LOGGER.info("Read and parse input file...");
        HashMap<String, LinkedList<Request>> user_requests
                = computeUserLog(parseFile(input_file, format));

        // Build graphs for each user
        ArrayList<String> user_list = new ArrayList<String>();
        for (Map.Entry<String, LinkedList<Request>> entry
                : user_requests.entrySet()) {
            String user = entry.getKey();
            File file = new File(output_dir.toString(), user + ".ser");
            if (overwrite_bool || !file.exists()) {
                LinkedList<Request> requests = entry.getValue();

                LinkedList<Graph<Domain>> graphs
                        = computeUserGraphs(k, user, requests, children_bool);

                // Store of the list of graphs for one user on disk
                saveGraphs(output_dir, user, graphs);
            } else {
                LOGGER.log(Level.INFO,
                        "User {0} has been skipped...", user);
            }
            user_list.add(user);
        }
        // Store usefull data for server
        saveUsers(output_dir, user_list);
        saveSubnet(output_dir, user_list);
        saveK(output_dir, k);
    }

    /**
     * Read and parse the input file line by line.
     *
     * @param file Input file
     * @param format File format (JSON or SQUID)
     * @return LinkedList&lt;Request&gt; : List of raw log file data
     */
    public final LinkedList<Request> parseFile(final InputStream file,
            final String format) {

        LinkedList<Request> requests = new LinkedList<Request>();
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(file, "UTF-8"));
            String line;

            while ((line = in.readLine()) != null) {
                requests.add(parseLine(line, format));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(BatchProcessor.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BatchProcessor.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return requests;
    }

    /**
     * Parse a given line.
     *
     * @param line Line of the log (one request)
     * @param format File format (JSON or SQUID)
     * @return Request : Object of the request contained in the line
     */
    public final Request parseLine(final String line, final String format) {
        Request request = null;
        try {
            if (format.equals("squid")) {
                request = parseLineSquid(line);
            } else if (format.equals("json")) {
                request = parseLineJson(line);
            } else {
                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
        }
        return request;
    }

    /**
     * Parse a give line encoded in squid format.
     *
     * @param line Line of the log (one request)
     * @return Request : Object of the request contained in the line
     */
    private Request parseLineSquid(final String line) {

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

    /**
     * Parse a given line encoded with JSON.
     *
     * @param line Line of the log (one request)
     * @return Request : Object of the request contained in the line
     */
    private Request parseLineJson(final String line) {
        JSONObject obj;
        try {
            obj = new JSONObject(line);
        } catch (JSONException ex) {
            throw new JSONException(ex + "\nJSON did not match " + line);
        }

        String thisdomain = null;

        try {
            thisdomain = computeDomain(obj.getString("tk_url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        Request request = null;
        try {
            SimpleDateFormat sdf
                    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(obj.getString("@timestamp"));
            Long timestamp = date.getTime();

            String client = "unkown";
            if (obj.has("tk_client_ip")) {
                client = obj.getString("tk_client_ip");
            }
            String method = "unkown";
            if (obj.has("tk_operation")) {
                method = obj.getString("tk_operation");
            }
            int bytes = 0;
            if (obj.has("tk_size")) {
                bytes = obj.getInt("tk_size");
            }
            String url = "unkown";
            if (obj.has("tk_url")) {
                url = obj.getString("tk_url");
            }
            String peerhost = "unkown";
            if (obj.has("tk_server_ip")) {
                peerhost = obj.getString("tk_server_ip");
            }
            String type = "unkown";
            if (obj.has("tk_mime_content")) {
                type = obj.getString("tk_mime_content");
            }

            request = new Request(
                    timestamp,
                    0, // info not available
                    client,
                    "unknown", // info not available
                    0, // info not available
                    bytes,
                    method,
                    url,
                    thisdomain,
                    "unknown", // info not available
                    peerhost,
                    type);

        } catch (ParseException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        return request;
    }

    /**
     * Associate each user (String) to his requests (LinkedList<Request>).
     *
     * @param requests_temp List of the raw log file
     * @return HashMap&lt;String, LinkedList&lt;Request&gt;&gt; : Map of the log
     * file sorted by user
     */
    final HashMap<String, LinkedList<Request>> computeUserLog(
            final LinkedList<Request> requests_temp) {
        HashMap<String, LinkedList<Request>> user_requests
                = new HashMap<String, LinkedList<Request>>();
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
     *
     * @param url URL
     * @return String : Domain name
     * @throws MalformedURLException if url is not correctly formed
     */
    private static String computeDomain(final String url)
            throws MalformedURLException {
        String url_temp = url;
        if (url_temp.startsWith("tcp://")) {
            String[] url_split = url_temp.split("[:]//");
            url_temp = url_split[1];
        }
        if (url_temp.startsWith("-://")) {
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
                    .log(Level.SEVERE, "URL " + url
                            + " is a malformed URL", ex);
        }
        if (domain.startsWith("www.")) {
            return domain.substring(4);
        }
        return domain;
    }

    /**
     * Compute the feature graphs of a user.
     *
     * @param k k value of k-NN Graphs
     * @param user User
     * @param requests List of requests for the given user
     * @param children_bool Boolean for children selection
     * @return LinkedList&lt;Graph&lt;Domain&gt;&gt; : List of the feature
     * graphs of a user
     * @throws IOException
     */
    final LinkedList<Graph<Domain>> computeUserGraphs(
            final int k,
            final String user,
            final LinkedList<Request> requests,
            final boolean children_bool) {
        LOGGER.log(Level.INFO,
                "Build the domains for user {0} ...", user);
        // Create the domain nodes
        // (it contains every requests of a specific domain, for each domain)
        HashMap<String, Domain> domains = computeDomainNodes(requests);

        LOGGER.log(Level.INFO,
                "Build the Time based graph for user {0} ...", user);
        Graph<Request> time_graph
                = computeRequestGraph(requests, k, new TimeSimilarity());
        // Selection of the temporal children only
        if (children_bool) {
            time_graph = childrenSelection(time_graph);
        }
        // Compute similarity between domains and build domain graph
        Graph<Domain> time_domain_graph
                = computeSimilarityDomain(time_graph, domains);

        LOGGER.log(Level.INFO,
                "Build the Domain based graph for user {0} ...", user);
        Graph<Request> domain_graph
                = computeRequestGraph(requests, k, new DomainSimilarity());
        // Selection of the temporal children only
        if (children_bool) {
            domain_graph = childrenSelection(domain_graph);
        }
        // Compute similarity between domains and build domain graph
        Graph<Domain> domain_domain_graph
                = computeSimilarityDomain(domain_graph, domains);

        /*LOGGER.log(Level.INFO,
                "Build the URL based graph for user {0} ...", user);
        Graph<Request> url_graph =
                computeRequestGraph(requests, k, new URLSimilarity());
        // Selection of the temporal children only
        if (children_bool) {
            url_graph = childrenSelection(url_graph);
        }
        // Compute similarity between domains and build domain graph
        Graph<Domain> url_domain_graph =
                computeSimilarityDomain(url_graph, domains);*/
        // List of graphs
        LinkedList<Graph<Domain>> graphs
                = new LinkedList<Graph<Domain>>();
        graphs.add(time_domain_graph);
        graphs.add(domain_domain_graph);
        //graphs.add(url_domain_graph);

        return graphs;
    }

    /**
     * Compute the graph of requests based on given similarity definition.
     *
     * @param requests List of requests
     * @param k k value of k-NN Graphs
     * @param Similarity : Similarity to be used in the graph
     * @return Graph&lt;Request&gt; : k-NN Graph of requests
     */
    final Graph<Request> computeRequestGraph(
            final LinkedList<Request> requests,
            final int k,
            final SimilarityInterface<Request> similarity) {
        Graph<Request> graph;
        if (requests.size() < 2 * k) {
            Brute<Request> nndes = new Brute<Request>();
            nndes.setSimilarity(similarity);
            nndes.setK(k);
            graph = nndes.computeGraph(requests);
        } else if (requests.size() >= 2 * k && requests.size() < 500) {
            NNDescent<Request> nndes
                    = new NNDescent<Request>();
            nndes.setSimilarity(similarity);
            nndes.setK(k);
            graph = nndes.computeGraph(requests);
        } else {
            ThreadedNNDescent<Request> nndes
                    = new ThreadedNNDescent<Request>();
            nndes.setSimilarity(similarity);
            nndes.setK(k);
            graph = nndes.computeGraph(requests);
        }

        return graph;
    }

    /**
     * Select only the temporal children.
     *
     * @param graph k-NN Graph of requests with temporal children
     * @return graph : k-NN Graph of requests without temporal children
     */
    final Graph<Request> childrenSelection(
            final Graph<Request> graph) {
        Graph<Request> graph_new = new Graph<Request>(Integer.MAX_VALUE);
        for (Request req : graph.getNodes()) {
            NeighborList neighbors_new = new NeighborList(Integer.MAX_VALUE);
            NeighborList neighbors = graph.getNeighbors(req);
            for (Neighbor<Request> neighbor : neighbors) {
                if (req.getTime() <= neighbor.getNode().getTime()) {
                    neighbors_new.add(neighbor);
                }
            }
            graph_new.put(req, neighbors_new);
        }
        return graph_new;
    }

    /**
     * Group the requests by domain to create domain nodes.
     *
     * @param requests List of requests
     * @return domains : Map of domains associating domain name to his list of
     * requests
     */
    final HashMap<String, Domain> computeDomainNodes(
            final LinkedList<Request> requests) {
        // Associate each domain_name (String) to a Domain
        HashMap<String, Domain> domains
                = new HashMap<String, Domain>();
        for (Request request : requests) {
            String domain_name = request.getDomain();

            Domain domain_node;
            if (domains.containsKey(domain_name)) {
                domain_node = domains.get(domain_name);

            } else {
                domain_node = new Domain();
                domain_node.setName(domain_name);
                domains.put(domain_name, domain_node);
            }

            domain_node.add(request);

        }

        return domains;
    }

    /**
     * Compute the similarity between domains and build domain graph.
     *
     * @param graph k-NN Graph of requests
     * @param domains Map of domains associating domain name to his list of
     * requests
     * @return domain_graph : k-NN Graph of domains based on the given k-NN
     * Graph of requests (Similarities between domains are the sum of the
     * similarities between their requests)
     */
    final Graph<Domain> computeSimilarityDomain(
            final Graph<Request> graph,
            final HashMap<String, Domain> domains) {
        // A domain is (for now) a list of Request.
        Graph<Domain> domain_graph = new Graph<Domain>(Integer.MAX_VALUE);

        // For each domain
        for (Map.Entry<String, Domain> domain_entry : domains.entrySet()) {

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            String domain_name = domain_entry.getKey();
            Domain domain_node = domain_entry.getValue();

            HashMap<Domain, Double> other_domains_sim
                    = new HashMap<Domain, Double>();

            // For each request in this domain
            for (Request request_node : domain_node) {

                // Check each neighbor
                NeighborList neighbors
                        = graph.getNeighbors(request_node);
                for (Neighbor<Request> neighbor : neighbors) {
                    // Find the corresponding domain name
                    String other_domain_name = neighbor.getNode().getDomain();
                    if (other_domain_name.equals(domain_name)) {
                        continue;
                    }

                    Domain other_domain = domains.get(other_domain_name);
                    double new_similarity = neighbor.getSimilarity();
                    if (other_domains_sim.containsKey(other_domain)) {
                        new_similarity
                                += other_domains_sim.get(other_domain);
                    }

                    if (new_similarity != 0) {
                        other_domains_sim.put(other_domain, new_similarity);
                    }
                }
            }

            NeighborList this_domain_neighbors
                    = new NeighborList(Integer.MAX_VALUE);
            for (Map.Entry<Domain, Double> other_domain_entry
                    : other_domains_sim.entrySet()) {
                this_domain_neighbors.add(new Neighbor(
                        other_domain_entry.getKey(),
                        other_domain_entry.getValue()));
            }

            domain_graph.put(domain_node, this_domain_neighbors);

        }
        return domain_graph;
    }

    /**
     * Save the feature graphs of a user.
     *
     * @param output_dir Output directory
     * @param user User
     * @param graphs List of the feature graphs
     * @throws IOException
     */
    final void saveGraphs(
            final Path output_dir,
            final String user,
            final LinkedList<Graph<Domain>> graphs) {

        try {
            LOGGER.log(Level.INFO,
                    "Save graphs of user {0} to disk...", user);
            File file = new File(output_dir.toString(), user + ".ser");
            if (Files.notExists(output_dir)) {
                Files.createDirectory(output_dir);
            }
            FileOutputStream output_stream
                    = new FileOutputStream(file.toString());
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
     *
     * @param output_dir Output directory
     * @param user_list List of users
     */
    final void saveUsers(
            final Path output_dir,
            final ArrayList<String> user_list) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of users to disk...");
            File file = new File(output_dir.toString(), "users.ser");
            FileOutputStream output_stream
                    = new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            output.writeObject(Subnet.sortIPs(user_list));
            output.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    /**
     * Compute the subnet list and Save the list of subnets.
     *
     * @param output_dir Output directory
     * @param user_list List of users
     */
    final void saveSubnet(
            final Path output_dir,
            final ArrayList<String> user_list) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of subnets to disk...");
            File file = new File(output_dir.toString(), "subnets.ser");
            FileOutputStream output_stream
                    = new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            // Compute the subnet list
            ArrayList<String> subnet_list = Subnet.getAllSubnets(user_list);
            output.writeObject(subnet_list);
            output.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    /**
     * Save the value of k (of k-NN Graphs).
     *
     * @param output_dir Output directory
     * @param k k value of k-NN Graphs
     */
    final void saveK(
            final Path output_dir,
            final int k) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of k value to disk...");
            File file = new File(output_dir.toString(), "k.ser");
            FileOutputStream output_stream
                    = new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            output.writeInt(k);
            output.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
