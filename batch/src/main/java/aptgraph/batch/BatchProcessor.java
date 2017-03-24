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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

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
     * @param format
     * @param children_bool
     * @param overwrite_bool
     * @throws IOException if we cannot read the input file
     */
    public final void analyze(final int k,
            final InputStream input_file, final Path output_dir,
            final String format,
            final boolean children_bool,
            final boolean overwrite_bool)
            throws IOException {

        // Parsing of the log file and Split of the log file by users
        LOGGER.info("Read and parse input file...");
        HashMap<String, LinkedList<Request>> user_requests =
                computeUserLog(parseFile(input_file, format));

        // Build graphs for each user
        ArrayList<String> user_list = new ArrayList<String>();
        for (Map.Entry<String, LinkedList<Request>> entry
                : user_requests.entrySet()) {
            String user = entry.getKey();
            File file = new File(output_dir.toString(), user + ".ser");
            if (overwrite_bool || !file.exists()) {
                LinkedList<Request> requests = entry.getValue();

                LinkedList<Graph<Domain>> graphs =
                        computeUserGraphs(k, user, requests, children_bool);

                // Store of the list of graphs for one user on disk
                saveGraphs(output_dir, user, graphs);
            } else {
                LOGGER.log(Level.INFO,
                "User {0} has been skipped...", user);
            }
            user_list.add(user);
        }
        saveUsers(output_dir, user_list);
        saveSubnet(output_dir, user_list);
        saveK(output_dir, k);
    }

    /**
     * Read and parse the input file line by line.
     * @param file
     * @param format
     * @return LinkedList<Request>
     */
    final LinkedList<Request> parseFile(final InputStream file,
            final String format)
            throws IOException {

        LinkedList<Request> requests = new LinkedList<Request>();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(file, "UTF-8"));
        String line = null;

        while ((line = in.readLine()) != null) {
            try {
                if (format.equals("squid")) {
                    requests.add(parseLineSquid(line));
                } else if (format.equals("json")) {
                    requests.add(parseLineJson(line));
                } else {
                    throw new IllegalArgumentException();
                }

            } catch (IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
            }
        }

        return requests;
    }

    /**
     * Parse a give line encoded in squid format.
     * @param line
     * @return  Request
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
     * @param line
     * @return Request
     */
    private Request parseLineJson(final String line) {
        JSONObject obj = new JSONObject(line);

        String thisdomain = null;

        try {
            thisdomain = computeDomain(obj.getString("tk_url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(BatchProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        Request request = null;
        try {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
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
     * @param children_bool
     * @return LinkedList<Graph<Domain>>
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
                "Build the time based graph for user {0} ...", user);
        Graph<Request> time_graph =
                computeRequestGraph(requests, k, new TimeSimilarity());
        // Selection of the temporal children only
        if (children_bool) {
            time_graph = childrenSelection(time_graph);
        }
        // Compute similarity between domains and build domain graph
        Graph<Domain> time_domain_graph =
                computeSimilarityDomain(time_graph, domains);

        LOGGER.log(Level.INFO,
                "Build the Domain based graph for user {0} ...", user);
        Graph<Request> domain_graph =
                computeRequestGraph(requests, k, new DomainSimilarity());
        // Selection of the temporal children only
        if (children_bool) {
            domain_graph = childrenSelection(domain_graph);
        }
        // Compute similarity between domains and build domain graph
        Graph<Domain> domain_domain_graph =
                computeSimilarityDomain(domain_graph, domains);

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
        LinkedList<Graph<Domain>> graphs =
                new LinkedList<Graph<Domain>>();
        graphs.add(time_domain_graph);
        graphs.add(domain_domain_graph);
        //graphs.add(url_domain_graph);

        return graphs;
    }

    /**
     * Compute the graph of requests based on given similarity definition.
     * @param requests
     * @param k
     * @param Similarity
     * @return Graph<Request>
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
        } else {
            ThreadedNNDescent<Request> nndes =
                    new ThreadedNNDescent<Request>();
            nndes.setSimilarity(similarity);
            nndes.setK(k);
            graph = nndes.computeGraph(requests);
        }

        return graph;
    }

    /**
     * Select only the temporal children.
     * @param graph
     * @return graph
     */
    final Graph<Request> childrenSelection(
            final Graph<Request> graph) {
        Graph<Request> graph_new = new Graph<Request>(Integer.MAX_VALUE);
        for (Request req : graph.getNodes()) {
            NeighborList neighbors_new = new NeighborList(Integer.MAX_VALUE);
            NeighborList neighbors = graph.getNeighbors(req);
            for (Neighbor<Request> neighbor : neighbors) {
                if (req.getTime() <= neighbor.node.getTime()) {
                    neighbors_new.add(neighbor);
                }
            }
            graph_new.put(req, neighbors_new);
        }
        return graph_new;
    }

    /**
     * Group the requests by domain to create domain nodes.
     * @param requests
     * @return domains
     */
    final HashMap<String, Domain> computeDomainNodes(
            final LinkedList<Request> requests) {
        // Associate each domain_name (String) to a Domain
        HashMap<String, Domain> domains =
                new HashMap<String, Domain>();
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
     * @param graph
     * @param domains
     * @return domain_graph
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

            HashMap<Domain, Double> other_domains_sim =
                    new HashMap<Domain, Double>();

            // For each request in this domain
            for (Request request_node : domain_node) {

                // Check each neighbor
                NeighborList neighbors =
                        graph.getNeighbors(request_node);
                for (Neighbor<Request> neighbor : neighbors) {
                    // Find the corresponding domain name
                    String other_domain_name = neighbor.node.getDomain();
                    if (other_domain_name.equals(domain_name)) {
                        continue;
                    }

                    Domain other_domain = domains.get(other_domain_name);
                    double new_similarity = neighbor.similarity;
                    if (other_domains_sim.containsKey(other_domain)) {
                        new_similarity +=
                                other_domains_sim.get(other_domain);
                    }

                    if (new_similarity != 0) {
                        other_domains_sim.put(other_domain, new_similarity);
                    }
                }
            }

            NeighborList this_domain_neighbors =
                    new NeighborList(Integer.MAX_VALUE);
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
     * @param output_dir
     * @param user
     * @param graphs
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
            Subnet sn = new Subnet();
            output.writeObject(sn.sortIPs(user_list));
            output.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }
    }

    /**
     * Save the list of subnets.
     * @param output_dir
     * @param user_list
     */
    final void saveSubnet(
            final Path output_dir,
            final ArrayList<String> user_list) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of subnets to disk...");
            File file = new File(output_dir.toString(), "subnets.ser");
            FileOutputStream output_stream =
                new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            Subnet sn = new Subnet();
            ArrayList<String> subnet_list = sn.getAllSubnets(user_list);
            output.writeObject(subnet_list);
            output.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }
    }

    /**
     * Save the value of k (of k-NN Graphs).
     * @param output_dir
     * @param k
     */
    final void saveK(
            final Path output_dir,
            final int k) {
        try {
            LOGGER.log(Level.INFO,
                    "Save list of k value to disk...");
            File file = new File(output_dir.toString(), "k.ser");
            FileOutputStream output_stream =
                new FileOutputStream(file.toString());
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(output_stream));
            output.writeInt(k);
            output.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }
    }
}
