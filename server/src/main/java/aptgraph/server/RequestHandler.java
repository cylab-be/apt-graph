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
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final Memory m = new Memory();

    RequestHandler(final Path input_dir) {
        this.input_dir = input_dir;
    }

    /**
     * Give access to the internal memory of server.
     * @return m
     */
    final Memory getMemory() {
        return this.m;
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
        String user = "219.253.194.242";
        Graph<Domain> graph = FileManager.getUserGraphs(
                input_dir, user).getFirst();

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
        LOGGER.info("Reading list of subnets from disk...");
        try {
            File file = new File(input_dir.toString(), "subnets.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            m.setAllSubnetsList((ArrayList<String>) input.readObject());
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        LOGGER.info("Reading list of users from disk...");
        try {
            File file = new File(input_dir.toString(), "users.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            m.setAllUsersList((ArrayList<String>) input.readObject());
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        ArrayList<String> output = new ArrayList<String>();
        output.addAll(m.getAllSubnetsList());
        output.addAll(m.getAllUsersList());
        return output;
    }

    /**
     * Analyze the graph of a specific user.
     * @param user
     * @param feature_ordered_weights
     * @param prune_threshold_temp
     * @param feature_weights
     * @param max_cluster_size_temp
     * @param prune_z_bool
     * @param cluster_z_bool
     * @param whitelist_bool
     * @param white_ongo
     * @param ranking_weights
     * @param apt_search
     * @return Output
     */
    public final Output analyze(
            final String user,
            final double[] feature_weights,
            final double[] feature_ordered_weights,
            final double prune_threshold_temp,
            final double max_cluster_size_temp,
            final boolean prune_z_bool,
            final boolean cluster_z_bool,
            final boolean whitelist_bool,
            final String white_ongo,
            final double[] ranking_weights,
            final boolean apt_search) {

        long start_time = System.currentTimeMillis();

        // Update users list and subnets list if needed
        if (m.getAllUsersList() == null || m.getAllSubnetsList() == null) {
            getUsers();
        }

        // Check input of the user
        if (!checkInputUser(user, feature_weights, feature_ordered_weights,
                prune_threshold_temp, max_cluster_size_temp,
                prune_z_bool, cluster_z_bool, ranking_weights)) {
            return null;
        }
        boolean[] stages = checkInputChanges(user, feature_weights,
                feature_ordered_weights, prune_threshold_temp,
                max_cluster_size_temp, prune_z_bool,
                cluster_z_bool, whitelist_bool, white_ongo,
                ranking_weights, apt_search);
        m.setCurrentK(FileManager.getK(input_dir));
        m.setUser(user);
        m.setFeatureWeights(feature_weights);
        m.setFeatureOrderedWeights(feature_ordered_weights);
        m.setPruningThresholdTemp(prune_threshold_temp);
        m.setMaxClusterSizeTemp(max_cluster_size_temp);
        m.setPruneZBool(prune_z_bool);
        m.setClusterZBool(cluster_z_bool);
        m.setWhitelistBool(whitelist_bool);
        m.setWhiteOngo(white_ongo);
        m.setRankingWeights(ranking_weights);
        m.setAptSearch(apt_search);

        long estimated_time_1 = System.currentTimeMillis() - start_time;
        System.out.println("1: " + estimated_time_1 + " (User input checked)");

        // Create the list of users used to produce final graph
        if (stages[0]) {
            if (m.getUser().equals("0.0.0.0")) {
                m.setUsersList(m.getAllUsersList());
            } else if (Subnet.isSubnet(m.getUser())) {
                m.setUsersList(Subnet.getUsersInSubnet(
                       m.getUser(), m.getAllUsersList()));
            } else {
                m.setUsersList(new ArrayList<String>()
                { { add(m.getUser()); } });
            }

            // Load users graphs
            loadUsersGraphs(start_time);

            long estimated_time_2 = System.currentTimeMillis() - start_time;
            System.out.println("2: " + estimated_time_2 + " (Data loaded)");

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
        }
        if (stages[1]) {
            // Compute each user graph
            LinkedList<Graph<Domain>> merged_graph_users
                    = computeUsersGraph();

            long estimated_time_3 = System.currentTimeMillis() - start_time;
            System.out.println("3: " + estimated_time_3
                    + " (Fusion of features done)");

            // Fusion of the users (Graph of Domains)
            double[] users_weights = new double[merged_graph_users.size()];
            for (int i = 0; i < merged_graph_users.size(); i++) {
                users_weights[i] = 1.0 / merged_graph_users.size();
            }
            m.setMergedGraph(computeFusionGraphs(merged_graph_users, "",
                            users_weights, new double[] {0.0}, "all"));

            long estimated_time_4 = System.currentTimeMillis() - start_time;
            System.out.println("4: " + estimated_time_4
                + " (Fusion of users done)");
        }
        if (stages[2]) {
            ArrayList<Double> similarities = listSimilarities();
            m.setMeanVarSimilarities(Utility.getMeanVariance(similarities));

            computeHistData(similarities, false, "prune");

            long estimated_time_5 = System.currentTimeMillis() - start_time;
            System.out.println("5: " + estimated_time_5
                    + " (Similarities hist. created)");
        }

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        m.setStdout("<pre>Number of users selected: "
                + m.getUsersList().size());
        m.concatStdout("<br>k-NN Graph: k: " + m.getCurrentK());
        m.concatStdout("<br>Total number of domains: "
                        + m.getAllDomains().get("all").values().size());

        if (stages[3]) {
            Graph<Domain> pruned_graph = new Graph<Domain>(m.getMergedGraph());
            // Prune
            pruned_graph = doPruning(pruned_graph, start_time);

            long estimated_time_6 = System.currentTimeMillis() - start_time;
            System.out.println("6: " + estimated_time_6 + " (Pruning done)");

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            // Clustering
            m.setClusters(pruned_graph.connectedComponents());

            long estimated_time_7 = System.currentTimeMillis() - start_time;
            System.out.println("7: " + estimated_time_7
                    + " (Clustering done)");
        }

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
        if (stages[4]) {
            ArrayList<Double> cluster_sizes = listClusterSizes(m.getClusters());
            m.setMeanVarClusters(Utility.getMeanVariance(cluster_sizes));

            computeHistData(cluster_sizes, true, "cluster");

            long estimated_time_8 = System.currentTimeMillis() - start_time;
            System.out.println("8: " + estimated_time_8
                    + " (Clusters hist. created)");
        }

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        if (stages[5]) {
            // Filtering
            doFiltering(start_time);

            long estimated_time_9 = System.currentTimeMillis() - start_time;
            System.out.println("9: " + estimated_time_9
                    + " (Filtering done)");
        }

        if (stages[6]) {
            // White listing
            if (m.getWhitelistBool()) {
                whiteListing();
            } else {
                m.setFilteredWhiteListed(m.getFiltered());
            }

            long estimated_time_10 = System.currentTimeMillis() - start_time;
            System.out.println("10: " + estimated_time_10
                    + " (White listing done)");
        }

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        if (stages[7]) {
            // Ranking
            showRanking();

            long estimated_time_11 = System.currentTimeMillis() - start_time;
            System.out.println("11: " + estimated_time_11
                    + " (Ranking printed)");

            m.concatStdout("<br>Found " + m.getFilteredWhiteListed().size()
                + " clusters</pre>");
        }

        // Output
        return createOutput();
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
        if (!m.getAllUsersList().contains(user)
                && !m.getAllSubnetsList().contains(user)) {
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
     * Determine which stage of the algorithm as to be computed.
     * @param user
     * @param k
     * @param feature_weights
     * @param feature_ordered_weights
     * @param prune_threshold_temp
     * @param max_cluster_size_temp
     * @param prune_z_bool
     * @param cluster_z_bool
     * @param whitelist_bool
     * @param white_ongo
     * @param ranking_weights
     * @param apt_search
     * @return boolean[]
     */
    private boolean[] checkInputChanges(
        final String user,
        final double[] feature_weights,
        final double[] feature_ordered_weights,
        final double prune_threshold_temp,
        final double max_cluster_size_temp,
        final boolean prune_z_bool,
        final boolean cluster_z_bool,
        final boolean whitelist_bool,
        final String white_ongo,
        final double[] ranking_weights,
        final boolean apt_search) {

        // By default all stages have changed
        boolean[] stages = {true, true, true, true, true, true, true, true};

        if (m.getUser().equals(user)) {
            stages[0] = false;

            if (Arrays.equals(m.getFeatureWeights(), feature_weights)
                    && Arrays.equals(m.getFeatureOrderedWeights(),
                            feature_ordered_weights)) {
                stages[1] = false;

                if (m.getPruneZBool() == prune_z_bool) {
                    stages[2] = false;

                    if (m.getPruningThresholdTemp() == prune_threshold_temp) {
                        stages[3] = false;

                        if (m.getClusterZBool() == cluster_z_bool) {
                            stages[4] = false;

                            if (m.getMaxClusterSizeTemp()
                                    == max_cluster_size_temp) {
                                stages[5] = false;

                                if (m.getWhitelistBool() == whitelist_bool
                                        && m.getWhiteOngo() != null
                                        && m.getWhiteOngo()
                                                .equals(white_ongo)) {
                                    stages[6] = false;

                                    if (Arrays.equals(m.getRankingWeights(),
                                            ranking_weights)
                                            && m.getAptSearch() == apt_search) {
                                        stages[7] = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return stages;
    }

    /**
     * Load the graphs needed.
     * @param start_time
     * @return
     */
    final void loadUsersGraphs(final long start_time) {
        for (String user_temp : m.getUsersList()) {
            LinkedList<Graph<Domain>> graphs_temp = FileManager.getUserGraphs(
                    input_dir, user_temp);

            // List all domains
            for (Domain dom : graphs_temp.getFirst().getNodes()) {
                m.getAllDomains().get("byUsers")
                        .put(user_temp + ":" + dom.getName(), dom);
                if (!m.getAllDomains().get("all").containsKey(dom.getName())) {
                    m.getAllDomains().get("all").put(dom.getName(), dom);
                } else if (!m.getAllDomains().get("all")
                        .get(dom.getName()).equals(dom)) {
                    m.getAllDomains().get("all").put(dom.getName(),
                            m.getAllDomains().get("all")
                                    .get(dom.getName()).merge(dom));
                }
            }

            // Store user graph
            m.getUsersGraphs().put(user_temp, graphs_temp);
        }
    }

    /**
     * Fusion features of each user.
     * @return merged_graph_users
     */
    final LinkedList<Graph<Domain>> computeUsersGraph() {
        LinkedList<Graph<Domain>> merged_graph_users
                    = new LinkedList<Graph<Domain>>();
        for (String user_temp : m.getUsersList()) {
            // Load user graph
            LinkedList<Graph<Domain>> graphs_temp
                    = m.getUsersGraphs().get(user_temp);

            // Fusion of the features (Graph of Domains)
            merged_graph_users.add(computeFusionGraphs(graphs_temp,
                    user_temp, m.getFeatureWeights(),
                    m.getFeatureOrderedWeights(), "byUsers"));
        }

        return merged_graph_users;
    }

    /**
     * Compute the fusion of graphs.
     * @param graphs
     * @param user
     * @param feature_weights
     * @param feature_ordered_weights
     * @param mode
     * @return merged_graph
     */
    final Graph<Domain> computeFusionGraphs(
            final LinkedList<Graph<Domain>> graphs,
            final String user,
            final double[] weights,
            final double[] ordered_weights,
            final String mode) {
        // Weighted average using parameter weights
        Graph<Domain> merged_graph = new Graph<Domain>(Integer.MAX_VALUE);
        for (Entry<String, Domain> entry_1 : m.getAllDomains()
                .get(mode).entrySet()) {
            String key = entry_1.getKey();
            Domain node = entry_1.getValue();
            if ((mode.equals("byUsers") && key.startsWith(user))
                    || mode.equals("all")) {
                // The json-rpc request was probably canceled by the user
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }

                HashMap<Domain, Double> all_neighbors =
                        new HashMap<Domain, Double>();

                for (int i = 0; i < graphs.size(); i++) {
                    Graph<Domain> graph_temp = graphs.get(i);
                    String user_temp = "";
                    if (mode.equals("all")) {
                        user_temp = graph_temp.getNodes().iterator()
                                .next().element().getClient();
                    }
                    String key_user = key;
                    if (mode.equals("all") && !key_user.startsWith(user_temp)) {
                        key_user = user_temp + ":" + key;
                    }
                    if (graph_temp.containsKey(m.getAllDomains()
                            .get("byUsers").get(key_user))) {
                        NeighborList neighbors_temp = graph_temp
                             .getNeighbors(m.getAllDomains().get("byUsers")
                                     .get(key_user));

                        for (Neighbor<Domain> neighbor_temp : neighbors_temp) {
                            double new_similarity =
                                    weights[i] * neighbor_temp.similarity;

                            if (mode.equals("byUsers") && all_neighbors
                                    .containsKey(neighbor_temp.node)) {
                                new_similarity +=
                                        all_neighbors.get(neighbor_temp.node);
                            } else if (mode.equals("all")
                                    && all_neighbors.containsKey(
                                            m.getAllDomains().get("all")
                                            .get(neighbor_temp.node
                                                    .getName()))) {
                                new_similarity +=
                                        all_neighbors.get(m.getAllDomains()
                                            .get("all").get(neighbor_temp.node
                                                    .getName()));
                            }

                            if (new_similarity != 0) {
                                if (mode.equals("all")) {
                                    all_neighbors.put(
                                      m.getAllDomains().get(mode)
                                      .get(neighbor_temp.node.getName()),
                                      new_similarity);
                                } else if (mode.equals("byUsers")) {
                                    all_neighbors.put(
                                      m.getAllDomains().get(mode)
                                      .get(user + ":"
                                              + neighbor_temp.node.getName()),
                                      new_similarity);
                                }
                            }

                        }
                    }
                }

                NeighborList nl = new NeighborList(Integer.MAX_VALUE);
                for (Entry<Domain, Double> entry_2 : all_neighbors.entrySet()) {
                    nl.add(new Neighbor(entry_2.getKey(), entry_2.getValue()));
                }
                merged_graph.put(node, nl);
            }
        }
        return merged_graph;
    }

    /**
     * Compute the list of all the similarities of domain graph.
     * @return similarities
     */
    final ArrayList<Double> listSimilarities() {
        ArrayList<Double> similarities = new ArrayList<Double>();
        for (Domain dom : m.getMergedGraph().getNodes()) {
            NeighborList neighbors = m.getMergedGraph().getNeighbors(dom);
            for (Neighbor<Domain> neighbor : neighbors) {
                similarities.add(neighbor.similarity);
            }
        }
        return similarities;
    }

    /**
     * Compute distribution of a list.
     * @param list
     * @param int_bool
     * @param mode (= prune OR cluster)
     * @return HashMap<Double, Integer>
     */
    private void computeHistData(
            final ArrayList<Double> list,
            final boolean int_bool,
            final String mode) {
        boolean z_bool;
        double mean;
        double variance;
        if (mode.equals("prune")) {
            z_bool = m.getPruneZBool();
            mean = m.getMeanVarSimilarities()[0];
            variance = m.getMeanVarSimilarities()[1];
        } else if (mode.equals("cluster")) {
            z_bool = m.getClusterZBool();
            mean = m.getMeanVarClusters()[0];
            variance = m.getMeanVarClusters()[1];
        } else {
            return;
        }
        ArrayList<Double> list_func = new ArrayList<Double>(list.size());
        // Transform list in z score if needed
        if (z_bool) {
            for (int i = 0; i <= list.size() - 1; i++) {
                list_func.add(i, Utility.getZ(mean, variance, list.get(i)));
            }
        } else {
            list_func = list;
        }
        ArrayList<Double> max_min = Utility.getMaxMin(list_func);
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
        if (mode.equals("prune")) {
            m.setHistDataSimilarities(hist_data);
        } else if (mode.equals("cluster")) {
            m.setHistDataClusters(hist_data);
        }
    }

    /**
     * Make the pruning on the graph and analyze the similarities.
     * @param graph
     * @param start_time
     * @return Graph<Domain>
     */
    final Graph<Domain> doPruning(
            final Graph<Domain> graph,
            final long start_time) {
        double prune_threshold;
        if (m.getPruneZBool()) {
            prune_threshold
                = Utility.computePruneThreshold(m.getMeanVarSimilarities()[0],
                        m.getMeanVarSimilarities()[1],
                        m.getPruningThresholdTemp());
        m.concatStdout("<br>Prune Threshold : ");
        m.concatStdout("<br>    Mean = " + m.getMeanVarSimilarities()[0]);
        m.concatStdout("<br>    Variance = " + m.getMeanVarSimilarities()[1]);
        m.concatStdout("<br>    Prune Threshold = " + prune_threshold);
        } else {
            prune_threshold = m.getPruningThresholdTemp();
        }
        graph.prune(prune_threshold);
        return graph;
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
     * Make the filtering and analyze the cluster sizes.
     * @param start_time
     * @return HistData
     */
    final void doFiltering(final long start_time) {
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        double max_cluster_size;
        if (m.getClusterZBool()) {
            max_cluster_size = Utility.computeClusterSize(
                    m.getMeanVarClusters()[0], m.getMeanVarClusters()[0],
                    m.getMaxClusterSizeTemp());
            m.concatStdout("<br>Cluster Size : ");
            m.concatStdout("<br>    Mean = " + m.getMeanVarClusters()[0]);
            m.concatStdout("<br>    Variance = " + m.getMeanVarClusters()[1]);
            m.concatStdout("<br>    Max Cluster Size = " + max_cluster_size);
        } else {
            max_cluster_size = m.getMaxClusterSizeTemp();
        }
        for (Graph<Domain> subgraph : m.getClusters()) {
            if (subgraph.size() <= max_cluster_size) {
                filtered.add(subgraph);
            }
        }
        m.setFiltered(filtered);
    }

    /**
     * White List unwanted domains.
     * @return
     */
    final void whiteListing() {
        LinkedList<Graph<Domain>> filtered_new
                = new LinkedList<Graph<Domain>>(m.getFiltered());

        List<String> whitelist = new ArrayList<String>();
        List<String> whitelist_ongo = new ArrayList<String>();
        LinkedList<Domain> whitelisted = new LinkedList<Domain>();
        try {
            whitelist = Files.readAllLines(m.getWhiteListPath(),
                            StandardCharsets.UTF_8);
            whitelist_ongo.addAll(Arrays.asList(m.getWhiteOngo().split("\n")));
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
                if ((whitelist.contains(dom.getName())
                        || whitelist_ongo.contains(dom.getName()))
                        && !whitelisted.contains(dom)) {
                    whitelisted.add(dom);
                }
            }
            Utility.remove(domain_graph, whitelisted);
        }

        m.concatStdout("<br>Number of white listed domains: "
                + whitelisted.size());
        m.setFilteredWhiteListed(filtered_new);
    }

    /**
     * Print of the ranking list.
     */
    private void showRanking() {
        //System.out.println("Stage 1");
        // Creation of a big graph with the result
        Graph<Domain> graph_all = new Graph<Domain>(Integer.MAX_VALUE);
        for (Graph<Domain> graph : m.getFilteredWhiteListed()) {
            for (Domain dom : graph.getNodes()) {
                if (!graph_all.containsKey(dom)) {
                    graph_all.put(dom, graph.getNeighbors(dom));
                } else {
                    NeighborList neighbors = graph_all.getNeighbors(dom);
                    neighbors.addAll(graph.getNeighbors(dom));
                    graph_all.put(dom, neighbors);
                }
            }
        }
        //System.out.println("Stage 2");
        // List all the remaining domains
        List<Domain> list_domain = new LinkedList<Domain>();
        for (Domain dom : graph_all.getNodes()) {
            list_domain.add(dom);
        }
        m.concatStdout("<br>Number of domains shown: " + list_domain.size());
        //System.out.println("Stage 3");
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
        //System.out.println("Stage 4");
        // Number of parents
        for (Domain parent : graph_all.getNodes()) {
            for (Neighbor<Domain> child : graph_all.getNeighbors(parent)) {
               index_parents.put(child.node, index_parents.get(child.node) + 1);
            }
        }
        //System.out.println("Stage 5");
        // Fusion of indexes
        HashMap<Domain, Double> index = new HashMap<Domain, Double>();
        for (Domain dom : graph_all.getNodes()) {
            index.put(dom,
                    m.getRankingWeights()[0] * index_parents.get(dom)
                    + m.getRankingWeights()[1] * index_children.get(dom)
                    + m.getRankingWeights()[2] * index_requests.get(dom));
        }
        //System.out.println("Stage 6");
        //Sort
        ArrayList<Domain> sorted = Utility.sortByIndex(list_domain, index);
        //System.out.println("Stage 7");
        // Print out
        if (m.getAptSearch()) {
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
                m.concatStdout("<br>TOP for APT.FINDME.be: "
                       + Math.round(top / m.getAllDomains()
                       .get("all").values().size() * 100 * 100) / 100.0 + "%");
            } else {
                m.concatStdout("<br>TOP for APT.FINDME.be: NOT FOUND");
            }
        }
        m.concatStdout("<br>Ranking:");
        for (Domain dom : sorted) {
            m.concatStdout("<br>    ("
                    + Math.round(index.get(dom) * 100) / 100.0 + ") " + dom);
        }
        //System.out.println("Stage 8");
    }

    /**
     * Create the output variable.
     * @return Output
     */
    private Output createOutput() {
        Output output = new Output();
        output.setFiltered(m.getFilteredWhiteListed());
        output.setStdout(m.getStdout());
        output.setHistPruning(m.getHistDataSimilarities());
        output.setHistCluster(m.getHistDataClusters());
        return output;
    }
}
