/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
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

package aptgraph.server;

import aptgraph.core.Domain;
import aptgraph.core.Subnet;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final Path input_dir;

    private static final Logger LOGGER
            = Logger.getLogger(JsonRpcServer.class.getName());

    RequestHandler(final Path input_dir) {
        this.input_dir = input_dir;
    }

    // Path of the white list
    private static final Path PATH = Paths.get("./src/main/resources/hosts");

    // Define stdout on UI
    private String stdout = "";

    // Data (Storage variable)
    private String user_store = "";
    private ArrayList<String> users_list_store = new ArrayList<String>();
    private ArrayList<String> all_users_list_store = new ArrayList<String>();
    private ArrayList<String> all_subnets_list_store = new ArrayList<String>();
    private Graph<Domain> graph_store = new Graph<Domain>();
    private int k_store = 0;

    /**
     * Return list of selected users.
     * @return ArrayList<String>
     */
    public final ArrayList<String> getUsersListStore() {
        return users_list_store;
    }

    /**
     * Set list of selected users.
     * @param list
     */
    public final void setUsersListStore(final ArrayList<String> list) {
        users_list_store = list;
    }

    /**
     * A test json-rpc call, with no argument, that should return "hello".
     * @return
     */
    public final String test() {
        return "hello";
    }

    /**
     * A dummy method that returns some clusters of nodes and edges.
     * @return
     */
    public final List<Graph<Domain>> dummy() {
        Graph<Domain> graph = getUserGraphs("219.253.194.242").getFirst();

        // Feature Fusion

        // URL/Domain clustering

        // Prune & clustering
        graph.prune(0.9);
        ArrayList<Graph<Domain>> clusters = graph.connectedComponents();

        // Filtering
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < 10) {
                filtered.add(subgraph);
            }
        }
        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;
    }

    /**
     * Give the list of users available in the log.
     * @return List of users
     */
    public final ArrayList<String> getUsers() {
        ArrayList<String> subnet_list = new ArrayList<String>();
        LOGGER.info("Reading list of subnets from disk...");
        try {
            File file = new File(input_dir.toString(), "subnets.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            subnet_list = (ArrayList<String>) input.readObject();
            input.close();
            all_subnets_list_store = subnet_list;
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        ArrayList<String> user_list = new ArrayList<String>();
        LOGGER.info("Reading list of users from disk...");
        try {
            File file = new File(input_dir.toString(), "users.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            user_list = (ArrayList<String>) input.readObject();
            input.close();
            all_users_list_store = user_list;
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        ArrayList<String> output = new ArrayList<String>();
        output.addAll(subnet_list);
        output.addAll(user_list);
        return output;
    }

    /**
     * Analyze the graph of a specific user.
     * @param user
     * @param feature_ordered_weights
     * @param prune_threshold_temp
     * @param feature_weights
     * @param max_cluster_size_temp
     * @param children_bool
     * @param prune_z_bool
     * @param cluster_z_bool
     * @param whitelist_bool
     * @param white_ongo
     * @param ranking_weights
     * @return Output
     */
    public final Output analyze(
            final String user,
            final double[] feature_weights,
            final double[] feature_ordered_weights,
            final double prune_threshold_temp,
            final double max_cluster_size_temp,
            final boolean children_bool,
            final boolean prune_z_bool,
            final boolean cluster_z_bool,
            final boolean whitelist_bool,
            final String white_ongo,
            final double[] ranking_weights) {

        long start_time = System.currentTimeMillis();

        // Update users list and subnets list if needed
        if (all_users_list_store.isEmpty()
                || all_subnets_list_store.isEmpty()) {
            getUsers();
        }

        // Check input of the user
        if (!checkInputUser(user, feature_weights, feature_ordered_weights,
                prune_threshold_temp, max_cluster_size_temp,
                prune_z_bool, cluster_z_bool, ranking_weights)) {
            return null;
        }

        long estimated_time_1 = System.currentTimeMillis() - start_time;
        System.out.println("1: " + estimated_time_1 + " (User input checked)");

        // Create the list of users used to produce final graph
        if (users_list_store.isEmpty() || graph_store == null
                || k_store == 0 || !user_store.equals(user)) {
            user_store = user;
            k_store = getK();

            Subnet sn = new Subnet();
            if (user.equals("0.0.0.0")) {
                users_list_store = all_users_list_store;
            } else if (sn.isSubnet(user)) {
               users_list_store =
                       sn.getUsersInSubnet(user, all_users_list_store);
            } else {
                users_list_store.clear();
                users_list_store.add(user);
            }
        }

        stdout = "<pre>Number of users selected: " + users_list_store.size();
        stdout = stdout.concat("<br>k-NN Graph: k: " + k_store);

        // Compute each user graph
        LinkedList<Graph<Domain>> merged_graph_users
                = new LinkedList<Graph<Domain>>();
        int domains_total
                = computeUsersGraphs(merged_graph_users, feature_weights,
                        feature_ordered_weights, start_time);
        if (domains_total == -1) {
                return null;
        }

        stdout = stdout.concat("<br>Total number of domains: "
                    + domains_total);

        // Fusion of the users (Graph of Domains)
        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = computeFusionGraphs(merged_graph_users,
                        new double[] {0.0}, users_weights);
        graph_store = merged_graph;

        // Prune
        HistData hist_pruning = doPruning(merged_graph,
                start_time, prune_z_bool, prune_threshold_temp);

        long estimated_time_8 = System.currentTimeMillis() - start_time;
        System.out.println("8: " + estimated_time_8 + " (Pruning done)");

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // Clustering
        ArrayList<Graph<Domain>> clusters = merged_graph.connectedComponents();

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        long estimated_time_9 = System.currentTimeMillis() - start_time;
        System.out.println("9: " + estimated_time_9 + " (Clustering done)");

        // Filtering
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        HistData hist_cluster = doFiltering(clusters, start_time,
                cluster_z_bool, max_cluster_size_temp, filtered);

        long estimated_time_11 = System.currentTimeMillis() - start_time;
        System.out.println("11: " + estimated_time_11 + " (Filtering done)");

        // White listing
        if (whitelist_bool) {
            filtered = whiteListing(filtered, white_ongo);
        }

        long estimated_time_12 = System.currentTimeMillis() - start_time;
        System.out.println("12: " + estimated_time_12
                + " (White listing done)");

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // Ranking
        if (filtered.size() > 1) {
            showRanking(filtered, domains_total, ranking_weights);
        }

        long estimated_time_13 = System.currentTimeMillis() - start_time;
        System.out.println("13: " + estimated_time_13 + " (Ranking printed)");

        // Output
        stdout = stdout.concat("<br>Found " + filtered.size()
                + " clusters</pre>");
        return createOutput(filtered, hist_pruning, hist_cluster);
    }

    /**
     * Check input of user.
     * @param user
     * @param feature_weights
     * @param feature_ordered_weights
     * @param prune_threshold_temp
     * @param max_cluster_size_temp
     * @param prune_z_bool
     * @param cluster_z_bool
     * @param ranking_weights
     * @return True if no problem
     */
    private boolean checkInputUser(
            final String user,
            final double[] feature_weights,
            final double[] feature_ordered_weights,
            final double prune_threshold_temp,
            final double max_cluster_size_temp,
            final boolean prune_z_bool,
            final boolean cluster_z_bool,
            final double[] ranking_weights) {
        // Check if user exists
        if (!all_users_list_store.contains(user)
                && !all_subnets_list_store.contains(user)) {
            return false;
        }

        // Verify the non negativity of weights and
        // the sum of the weights of features
        double sum_feature_weights = 0;
        for (double d : feature_weights) {
            sum_feature_weights += d;
            if (d < 0) {
                return false;
            }
        }
        double sum_ordered_weights = 0;
        for (double d : feature_ordered_weights) {
            sum_ordered_weights += d;
            if (d < 0) {
                return false;
            }
        }
        if (sum_feature_weights != 1 || sum_ordered_weights != 1) {
            return false;
        }

        // Verify input of user for pruning
        if (!prune_z_bool && prune_threshold_temp < 0) {
            return false;
        }
        // Verify input of user for clustering
        if (!cluster_z_bool && max_cluster_size_temp < 0) {
            return false;
        }

        // Verify the non negativity of weights and
        // the sum of the weights for ranking
        double sum_ranking_weights = 0;
        for (double d : ranking_weights) {
            sum_ranking_weights += d;
            if (d < 0) {
                return false;
            }
        }

        if (sum_ranking_weights != 1) {
            return false;
        }

        return true;
    }

    /**
     * Load the list of graphs for a given user.
     * @param user
     * @return List of graphs
     */
    final LinkedList<Graph<Domain>> getUserGraphs(final String user) {
        LinkedList<Graph<Domain>> user_graphs =
                new LinkedList<Graph<Domain>>();
        LOGGER.log(Level.INFO, "Reading graphs of user {0} from disk...", user);
        try {
            File file = new File(input_dir.toString(), user + ".ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            user_graphs = (LinkedList<Graph<Domain>>) input.readObject();
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        return user_graphs;
    }

    /**
     * Load the value of k used for k-NN Graphs.
     * @return List of graphs
     */
    final int getK() {
        int k = 0;
        LOGGER.log(Level.INFO, "Reading k value from disk...");
        try {
            File file = new File(input_dir.toString(), "k.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            k = (int) input.readInt();
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }

        return k;
    }

    /**
     * Fusion features of each user.
     * @param merged_graph_users
     * @param feature_weights
     * @param feature_ordered_weights
     * @param start_time
     * @return domains_total
     */
    final int computeUsersGraphs(
            final LinkedList<Graph<Domain>> merged_graph_users,
            final double[] feature_weights,
            final double[] feature_ordered_weights,
            final long start_time) {
        int domains_total = 0;
        for (String user_temp : users_list_store) {
            LinkedList<Graph<Domain>> graphs_temp = getUserGraphs(user_temp);

            // Count number of domains
            for (Domain dom : graphs_temp.getFirst().getNodes()) {
                domains_total += 1;
            }

            long estimated_time_2 = System.currentTimeMillis() - start_time;
            System.out.println("2: " + estimated_time_2 + " (Data loaded)");

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return -1;
            }

            // Fusion of the features (Graph of Domains)
            Graph<Domain> merged_graph_temp = computeFusionGraphs(graphs_temp,
                            feature_ordered_weights, feature_weights);

            merged_graph_users.add(merged_graph_temp);

            long estimated_time_3 = System.currentTimeMillis() - start_time;
            System.out.println("3: " + estimated_time_3
                    + " (Fusion of features done)");
        }
        return domains_total;
    }

    /**
     * Compute the fusion of graphs.
     * @param graphs
     * @param feature_ordered_weights
     * @param feature_weights
     * @return merged_graph
     */
    final Graph<Domain> computeFusionGraphs(
            final LinkedList<Graph<Domain>> graphs,
            final double[] ordered_weights,
            final double[] weights) {

        // Weighted average using parameter weights
        Graph<Domain> merged_graph = new Graph<Domain>(Integer.MAX_VALUE);
        for (Domain node : graphs.getFirst().getNodes()) {

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            HashMap<Domain, Double> all_neighbors =
                    new HashMap<Domain, Double>();

            for (int i = 0; i < graphs.size(); i++) {
                Graph<Domain> graph_temp = graphs.get(i);
                NeighborList neighbors_temp =
                        graph_temp.getNeighbors(node);

                for (Neighbor<Domain> neighbor_temp : neighbors_temp) {
                    double new_similarity =
                            weights[i] * neighbor_temp.similarity;

                    if (all_neighbors.containsKey(neighbor_temp.node)) {
                        new_similarity +=
                                all_neighbors.get(neighbor_temp.node);
                    }

                    if (new_similarity != 0) {
                       all_neighbors.put(neighbor_temp.node, new_similarity);
                    }

                }
            }

            NeighborList nl = new NeighborList(Integer.MAX_VALUE);
            for (Entry<Domain, Double> entry : all_neighbors.entrySet()) {
                nl.add(new Neighbor(entry.getKey(), entry.getValue()));
            }

            merged_graph.put(node, nl);
        }

        return merged_graph;
    }

    /**
     * Make the pruning on the graph and analyze the similarities.
     * @param domain_graph
     * @param start_time
     * @param prune_z_bool
     * @param prune_threshold_temp
     * @return HistData
     */
    final HistData doPruning(
            final Graph<Domain> domain_graph,
            final long start_time,
            final boolean prune_z_bool,
            final double prune_threshold_temp) {
        ArrayList<Double> similarities = listSimilarities(domain_graph);
        ArrayList<Double> mean_var_prune = getMeanVariance(similarities);
        double mean_prune = mean_var_prune.get(0);
        double variance_prune = mean_var_prune.get(1);

        HistData hist_pruning = computeHistData(similarities, mean_prune,
                variance_prune, prune_z_bool, false);

        long estimated_time_7 = System.currentTimeMillis() - start_time;
        System.out.println("7: " + estimated_time_7
                + " (Similarities hist. created)");

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        double prune_threshold;
        if (prune_z_bool) {
            prune_threshold
                = computePruneThreshold(mean_prune, variance_prune,
                        prune_threshold_temp);
        } else {
            prune_threshold = prune_threshold_temp;
        }
        domain_graph.prune(prune_threshold);
        return hist_pruning;
    }

    /**
     * Compute the list of all the similarities of domain graph.
     * @param domain_graph
     * @return similarities
     */
    final ArrayList<Double> listSimilarities(
            final Graph<Domain> domain_graph) {
        ArrayList<Double> similarities = new ArrayList<Double>();
        for (Domain dom : domain_graph.getNodes()) {
            NeighborList neighbors = domain_graph.getNeighbors(dom);
            for (Neighbor<Domain> neighbor : neighbors) {
                similarities.add(neighbor.similarity);
            }
        }
        return similarities;
    }

    /**
     * Compute distribution of a list.
     * @param list
     * @param mean
     * @param variance
     * @param z_bool
     * @param int_bool
     * @return HashMap<Double, Integer>
     */
    private HistData computeHistData(
            final ArrayList<Double> list,
            final double mean, final double variance, final boolean z_bool,
            final boolean int_bool) {
        ArrayList<Double> list_func = new ArrayList<Double>(list.size());
        // Transform list in z score if needed
        if (z_bool) {
            for (int i = 0; i <= list.size() - 1; i++) {
                list_func.add(i, getZ(mean, variance, list.get(i)));
            }
        } else {
            list_func = list;
        }
        ArrayList<Double> max_min = getMaxMin(list_func);
        double max = max_min.get(0);
        double min = max_min.get(1);
        double step;
        if (int_bool) {
            max = Math.round(max);
            min = Math.round(min);
            step = 1.0;
        } else {
            if (!z_bool) {
                max = Math.min(5.0, max);
            }
            step = 0.01;
        }
        HistData hist_data = new HistData();
        for (Double i = min; i <= max + step; i += step) {
            hist_data.put(i, 0.0);
        }
        int total_links = list_func.size();
        for (Double d1 : list_func) {
            Double diff = Double.MAX_VALUE;
            Double bin = hist_data.keySet().iterator().next();
            for (Double d2 : hist_data.keySet()) {
                if (Math.abs(d2 - d1) < diff) {
                    diff = Math.abs(d2 - d1);
                    bin = d2;
                }
            }
            hist_data.put(bin, hist_data.get(bin) + 1.0 / total_links * 100);
        }
        // there ara actually (bins + 2) bins (to include max in the histogram)
        return hist_data;
    }

    /**
     * Compute the absolute prune threshold based on z score.
     * @param mean
     * @param variance
     * @param z_prune_threshold
     * @return prune_threshold
     */
    final double computePruneThreshold(final double mean,
            final double variance,
            final Double z_prune_threshold) {
        double prune_threshold = fromZ(mean, variance, z_prune_threshold);
        if (prune_threshold < 0) {
            prune_threshold = 0;
        }
        stdout = stdout.concat("<br>Prune Threshold : ");
        stdout = stdout.concat("<br>    Mean = " + mean);
        stdout = stdout.concat("<br>    Variance = " + variance);
        stdout = stdout.concat("<br>    Prune Threshold = " + prune_threshold);

        return prune_threshold;
    }

    /**
     * Make the filtering and analyze the cluster sizes.
     * @param clusters
     * @param start_time
     * @param cluster_z_bool
     * @param max_cluster_size_temp
     * @param filtered
     * @return HistData
     */
    final HistData doFiltering(
            final ArrayList<Graph<Domain>> clusters,
            final long start_time,
            final boolean cluster_z_bool,
            final double max_cluster_size_temp,
            final LinkedList<Graph<Domain>> filtered) {
        ArrayList<Double> cluster_sizes = listClusterSizes(clusters);
        ArrayList<Double> mean_var_cluster = getMeanVariance(cluster_sizes);
        double mean_cluster = mean_var_cluster.get(0);
        double variance_cluster = mean_var_cluster.get(1);

        HistData hist_cluster = computeHistData(cluster_sizes, mean_cluster,
                variance_cluster, cluster_z_bool, true);

        long estimated_time_10 = System.currentTimeMillis() - start_time;
        System.out.println("10: " + estimated_time_10
                + " (Clusters hist. created)");

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        double max_cluster_size;
        if (cluster_z_bool) {
            max_cluster_size = computeClusterSize(mean_cluster,
                    variance_cluster, max_cluster_size_temp);
        } else {
            max_cluster_size = max_cluster_size_temp;
        }
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() <= max_cluster_size) {
                filtered.add(subgraph);
            }
        }
        return hist_cluster;
    }

    /**
     * Compute the list of the sizes of clusters.
     * @param clusters
     * @return cluster_sizes
     */
    final ArrayList<Double> listClusterSizes(
        final ArrayList<Graph<Domain>> clusters) {
        ArrayList<Double> cluster_sizes = new ArrayList<Double>();
        for (Graph<Domain> subgraph : clusters) {
            cluster_sizes.add((double) subgraph.size());
        }
        return cluster_sizes;
    }

    /**
     * Compute the absolute maximum cluster size based on z score.
     * @param mean
     * @param variance
     * @param z_max_cluster_size
     * @return max_cluster_size
     */
    private double computeClusterSize(final double mean, final double variance,
            final Double z_max_cluster_size) {
        double max_cluster_size_temp = fromZ(mean, variance,
                z_max_cluster_size);
        int max_cluster_size = (int) Math.round(max_cluster_size_temp);
        if (max_cluster_size < 0) {
            max_cluster_size = 0;
        }
        stdout = stdout.concat("<br>Cluster Size : ");
        stdout = stdout.concat("<br>    Mean = " + mean);
        stdout = stdout.concat("<br>    Variance = " + variance);
        stdout =
              stdout.concat("<br>    Max Cluster Size = " + max_cluster_size);

        return max_cluster_size;
    }

    /**
     * White List unwanted domains.
     * @param domain_graph
     * @param white_ongo
     * @return domain_graph
     */
    final LinkedList<Graph<Domain>>
         whiteListing(final LinkedList<Graph<Domain>> filtered,
                 final String white_ongo) {
        LinkedList<Graph<Domain>> filtered_new = filtered;

        List<String> whitelist = new ArrayList<String>();
        List<String> whitelist_ongo = new ArrayList<String>();
        LinkedList<Domain> whitelisted = new LinkedList<Domain>();
        try {
            whitelist =
                    Files.readAllLines(PATH, StandardCharsets.UTF_8);
            whitelist_ongo.addAll(Arrays.asList(white_ongo.split("\n")));
        } catch (IOException ex) {
            Logger.getLogger(RequestHandler.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        Iterator<Graph<Domain>> iterator_1 = filtered_new.iterator();
        while (iterator_1.hasNext()) {
            Graph<Domain> domain_graph = iterator_1.next();
            Iterator<Domain> iterator_2 = domain_graph.getNodes().iterator();
            while (iterator_2.hasNext()) {
                Domain dom = iterator_2.next();
                if ((whitelist.contains(dom.toString())
                        || whitelist_ongo.contains(dom.toString()))
                        && !whitelisted.contains(dom)) {
                    whitelisted.add(dom);
                }
            }
            remove(domain_graph, whitelisted);
        }

        stdout = stdout.concat("<br>Number of white listed domains: "
                + whitelisted.size());
        return filtered_new;
    }

    /**
     * Remove a list of nodes from a given graph (and update graph).
     * @param <U>
     * @param graph
     * @param node
     */
    static <U> void remove(final Graph<U> graph, final LinkedList<U> nodes) {
        HashMap<U, NeighborList> map = graph.getHashMap();

        // Delete the node
        for (U node : nodes) {
            map.remove(node);
        }
        // Delete the invalid edges to avoid "NullPointerException"
        Iterator<Map.Entry<U, NeighborList>> iterator_1 =
                map.entrySet().iterator();
        while (iterator_1.hasNext()) {
            Map.Entry<U, NeighborList> entry = iterator_1.next();
            NeighborList neighborlist = entry.getValue();

            // Delete reference to deleted node
            // => for() can't be used
            // (see http://stackoverflow.com/questions/223918/)
            Iterator<Neighbor> iterator_2 = neighborlist.iterator();
            while (iterator_2.hasNext()) {
                Neighbor<U> neighbor = iterator_2.next();
                if (nodes.contains(neighbor.node)) {
                    iterator_2.remove();
                }
            }
        }
    }

    /**
     * Print of the ranking list.
     * @param filtered
     * @param domains_total
     * @param ranking_weights
     */
    private void showRanking(final LinkedList<Graph<Domain>> filtered,
            final int domains_total, final double[] ranking_weights) {
        // Creation of a big graph with the result
        Graph<Domain> graph_all = new Graph<Domain>(1000);
        for (Graph<Domain> graph : filtered) {
            for (Domain dom : graph.getNodes()) {
                graph_all.put(dom, graph.getNeighbors(dom));
            }
        }

        // List all the remaining domains
        List<Domain> list_domain = new LinkedList<Domain>();
        for (Domain dom : graph_all.getNodes()) {
            list_domain.add(dom);
        }
        stdout = stdout.concat("<br>Number of domains shown: "
                + list_domain.size());

        // Number of children
        HashMap<Domain, Integer> index_children =
                new HashMap<Domain, Integer>();
        // Number of parents
        HashMap<Domain, Integer> index_parents =
                new HashMap<Domain, Integer>();
        // Number of requests index
        HashMap<Domain, Integer> index_requests =
                new HashMap<Domain, Integer>();

        // Number of children & Number of requests
        for (Domain dom : graph_all.getNodes()) {
            index_children.put(dom, graph_all.getNeighbors(dom).size());
            index_parents.put(dom, 0);
            index_requests.put(dom, dom.size());
        }

        // Number of parents
        for (Domain parent : graph_all.getNodes()) {
            for (Neighbor<Domain> child : graph_all.getNeighbors(parent)) {
               index_parents.put(child.node, index_parents.get(child.node) + 1);
            }
        }

        // Fusion of indexes
        HashMap<Domain, Double> index = new HashMap<Domain, Double>();
        for (Domain dom : graph_all.getNodes()) {
            index.put(dom,
                    ranking_weights[0] * index_parents.get(dom)
                    + ranking_weights[1] * index_children.get(dom)
                    + ranking_weights[2] * index_requests.get(dom));
        }

        //Sort
        ArrayList<Domain> sorted = sortByIndex(list_domain, index);

        // Print out
        double top = 0.0;
        double rank = Double.MAX_VALUE;
        boolean founded = false;
        for (Domain dom : sorted) {
            if (dom.toString().equals("APT.FINDME.be")) {
                rank = index.get(dom);
                top++;
                founded = true;
            }
            if (!dom.toString().equals("APT.FINDME.be")
                    && index.get(dom) <= rank) {
                top++;
            }
        }
        if (founded) {
            stdout = stdout.concat("<br>TOP for APT.FINDME.be: "
                   + Math.round(top / domains_total * 100 * 100) / 100.0 + "%");
        } else {
            stdout = stdout.concat("<br>TOP for APT.FINDME.be: NOT FOUND");
        }
        stdout = stdout.concat("<br>Ranking:");
        stdout = stdout.concat("<br>(#Children + #Parents / #Resquests)");
        for (Domain dom : sorted) {
            stdout = stdout.concat("<br>    ("
                    + Math.round(index.get(dom) * 100) / 100.0 + ") " + dom);
        }
    }

    /**
     * Sorting function, based on the given indexes.
     * @param list_domain
     * @param index
     * @return ArrayList<Domain> sorted list
     */
    private ArrayList<Domain> sortByIndex(final List<Domain> list_domain,
            final HashMap<Domain, Double> index) {
        Domain selected;
        ArrayList<Domain> sorted = new ArrayList<Domain>();
        while (!list_domain.isEmpty()) {
            selected = list_domain.get(0);
            for (Domain dom : list_domain) {
                if (index.get(dom) < index.get(selected)) {
                    selected = dom;
                }
            }
            sorted.add(selected);
            list_domain.remove(selected);
        }

        return sorted;
    }

    /**
     * Create the output variable.
     * @param filtered
     * @param hist_pruning
     * @param hist_cluster
     * @return Output
     */
    private Output createOutput(
            final LinkedList<Graph<Domain>> filtered,
            final HistData hist_pruning,
            final HistData hist_cluster) {
        Output output = new Output();
        output.setFiltered(filtered);
        output.setStdout(stdout);
        output.setHistPruning(hist_pruning);
        output.setHistCluster(hist_cluster);
        return output;
    }

    /**
     * Compute the mean of an ArrayList<Double>.
     * @param list
     * @return mean
     */
    private double getMean(final ArrayList<Double> list) {
            double sum = 0.0;
            for (double i : list) {
                sum += i;
            }
            return sum / list.size();
    }

    /**
     * Compute the mean and variance of an ArrayList<Double>.
     * @param list
     * @return ArrayList<Double> mean_variance
     */
    final ArrayList<Double> getMeanVariance(final ArrayList<Double> list) {
        double mean = getMean(list);
        double sum = 0.0;
        for (double i :list) {
            sum += (i - mean) * (i - mean);
        }
        ArrayList<Double> out = new ArrayList<Double>(2);
        out.add(mean);
        out.add(sum / list.size());
        return out;
    }

    /**
     * Compute the z score of a value.
     * @param list
     * @param value
     * @return z
     */
    private double getZ(final double mean, final double variance,
            final Double value) {
        return (value - mean) / Math.sqrt(variance);
    }

    /**
     * Compute the absolute value from the z score.
     * @param list
     * @param z
     * @return absolute value
     */
    private double fromZ(final double mean, final double variance,
            final Double z) {
        return mean + z * Math.sqrt(variance);
    }

    /**
     * Compute maximum and minimum of an ArrayList.
     * @param list
     * @return ArrayList<Double> max_min
     */
    private ArrayList<Double> getMaxMin(final ArrayList<Double> list) {
        Double max = 0.0;
        Double min = Double.MAX_VALUE;
        for (Double d : list) {
            max = Math.max(d, max);
            min = Math.min(d, min);
        }
        ArrayList<Double> max_min = new ArrayList<Double>(2);
        max_min.add(max);
        max_min.add(min);
        return max_min;
    }
}
