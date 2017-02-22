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

import aptgraph.core.Request;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final HashMap<String,
            LinkedList<Graph<Request>>> user_graphs;
    private final InputStream hosts_file;

    RequestHandler(final HashMap<String,
            LinkedList<Graph<Request>>> user_graphs,
            final InputStream hosts_file) {
        this.user_graphs = user_graphs;
        this.hosts_file = hosts_file;
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
    public final List<Graph<Request>> dummy() {
        Graph<Request> graph = user_graphs.get("219.253.194.242").getFirst();

        // Feature Fusion

        // URL/Domain clustering

        // Prune & clustering
        graph.prune(0.9);
        ArrayList<Graph<Request>> clusters = graph.connectedComponents();

        // Filtering
        LinkedList<Graph<Request>> filtered = new LinkedList<Graph<Request>>();
        for (Graph<Request> subgraph : clusters) {
            if (subgraph.size() < 10) {
                filtered.add(subgraph);
            }
        }
        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;
    }

    /**
     *
     * @param user_temp
     * @param feature_ordered_weights
     * @param prune_threshold
     * @param feature_weights
     * @param max_cluster_size
     * @return
     */
    public final List<Graph<Domain>> analyze(
            final String user_temp,
            final double[] feature_weights,
            final double[] feature_ordered_weights,
            final double prune_threshold,
            final int max_cluster_size) {

        // START user selection
        // List of the user
        LinkedList<String> users = new LinkedList<String>();
        for (Map.Entry<String, LinkedList<Graph<Request>>> entry_set
                : user_graphs.entrySet()) {
            String key = entry_set.getKey();
            users.add(key);
        }
        System.out.println("List of user : " + users);

        // Choice of the graphs of the user(need to be choosed on the web page)
        String user = users.getFirst(); // a remplacer par user_temp
        // END user selection
        LinkedList<Graph<Request>> graphs = user_graphs.get(user);

        // Verify the sum of the weights
        double sum_feature_weights = 0;
        for (double d : feature_weights) {
            sum_feature_weights += d;
        }
        double sum_ordered_weights = 0;
        for (double d : feature_ordered_weights) {
            sum_ordered_weights += d;
        }
        if (sum_feature_weights != 1 || sum_ordered_weights != 1) {
            System.out.println("Error with weights");
            return null;
        }

        // Fusion of the features (Graph of Requests)
        Graph<Request> merged_graph =
                computeFusionFeatures(graphs,
                        feature_ordered_weights, feature_weights);

        // Selection of the temporal children only
        merged_graph = childrenSelection(merged_graph);

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // From Graph of Requests to HashMap of Domains
        // (it contains every requests of a specific domain, for each domain)
        HashMap<String, Domain> domains =
                computeDomainGraph(merged_graph);

        // Compute similarity between domains and build domain graph
        Graph<Domain> domain_graph =
                computeSimilarityDomain(merged_graph, domains);

        // White listing
        // START PROBLEM
        remove(domain_graph, domain_graph.first());
        System.out.println("SUCCES");
        // STOP PROBLEM
        // domain_graph = whiteListing(domain_graph);

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // Prune
        showSimilaritiesInfo(domain_graph, prune_threshold);
        domain_graph.prune(prune_threshold);

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // Clustering
        ArrayList<Graph<Domain>> clusters = domain_graph.connectedComponents();

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // Filtering
        showClusterSizeInfo(clusters, max_cluster_size);
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < max_cluster_size) {
                filtered.add(subgraph);
            }
        }

        showRankingList(filtered);
        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;
    }

    final Graph<Request> computeFusionFeatures(
            final LinkedList<Graph<Request>> graphs,
            final double[] feature_ordered_weights,
            final double[] feature_weights) {

        int k = graphs.getFirst().getK();

        // Feature Fusion
        // Weighted average using parameter feature_weights
        Graph<Request> merged_graph = new Graph<Request>(k);
        for (Request node : graphs.getFirst().getNodes()) {

            // The json-rpc request was probably canceled by the user
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            HashMap<Request, Double> all_neighbors =
                    new HashMap<Request, Double>();

            for (int i = 0; i < graphs.size(); i++) {
                Graph<Request> feature_graph = graphs.get(i);
                NeighborList feature_neighbors =
                        feature_graph.getNeighbors(node);

                for (Neighbor<Request> feature_neighbor : feature_neighbors) {
                    double new_similarity =
                            feature_weights[i] * feature_neighbor.similarity;

                    if (all_neighbors.containsKey(feature_neighbor.node)) {
                        new_similarity +=
                                all_neighbors.get(feature_neighbor.node);
                    }

                    all_neighbors.put(feature_neighbor.node, new_similarity);

                }
            }

            NeighborList nl = new NeighborList(k);
            for (Entry<Request, Double> entry : all_neighbors.entrySet()) {
                nl.add(new Neighbor(entry.getKey(), entry.getValue()));
            }

            merged_graph.put(node, nl);
        }

        return merged_graph;
    }

    final HashMap<String, Domain> computeDomainGraph(
            final Graph<Request> merged_graph) {
        // URL/Domain clustering
        // Associate each domain_name (String) to a Node<Domain>
        HashMap<String, Domain> domains =
                new HashMap<String, Domain>();
        for (Request node : merged_graph.getNodes()) {
            String domain_name = node.getDomain();

            Domain domain_node;
            if (domains.containsKey(domain_name)) {
                domain_node = domains.get(domain_name);

            } else {
                domain_node = new Domain();
                domain_node.setName(domain_name);
                domains.put(domain_name, domain_node);
            }

            domain_node.add(node);

        }

        return domains;
    }

    final Graph<Domain> computeSimilarityDomain(
            final Graph<Request> merged_graph,
            final HashMap<String, Domain> domains) {
        // A domain is (for now) a list of Request.
        Graph<Domain> domain_graph = new Graph<Domain>(Integer.MAX_VALUE);

        // For each domain
        for (Entry<String, Domain> domain_entry : domains.entrySet()) {

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
                        merged_graph.getNeighbors(request_node);
                for (Neighbor<Request> neighbor : neighbors) {
                    Request target_request = neighbor.node;

                    // Find the corresponding domain name
                    String other_domain_name = target_request.getDomain();
                    if (other_domain_name.equals(domain_name)) {
                        continue;
                    }

                    Domain other_domain = domains.get(other_domain_name);
                    double new_similarity = neighbor.similarity;
                    if (other_domains_sim.containsKey(other_domain)) {
                        new_similarity +=
                                other_domains_sim.get(other_domain);
                    }

                    other_domains_sim.put(other_domain, new_similarity);
                }
            }

            NeighborList this_domain_neighbors =
                    new NeighborList(1000);
            for (Entry<Domain, Double> other_domain_entry
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
     * Select only the temporal children.
     * @param graph
     * @return graph
     */
    private Graph<Request> childrenSelection(
            final Graph<Request> graph) {
        Graph<Request> graph_new = new Graph<Request>();
        for (Request req : graph.getNodes()) {
            NeighborList neighbors_new = new NeighborList(1000);
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
     * Print of the ranking list.
     * @param filtered
     */
    private void showRankingList(final LinkedList<Graph<Domain>> filtered) {
        HashMap<Domain, Integer> index = new HashMap<Domain, Integer>();

        for (Graph<Domain> graph : filtered) {
            for (Domain dom : graph.getNodes()) {
                if (!index.containsKey(dom)) {
                    index.put(dom, graph.getNeighbors(dom).size());
                }
            }
        }
        Map<Domain, Integer> ranking = sortByValue(index);
        System.out.println("Ranking List #Children(#Requests) =");
        for (Map.Entry<Domain, Integer> entry : ranking.entrySet()) {
            System.out.println("    " + entry.getValue()
                    + "(" + entry.getKey().size()
                    + ") : " + entry.getKey());
        }
    }

    /**
     * Sorting function (source : https://stackoverflow.com/questions/109383/
     * sort-a-mapkey-value-by-values-java).
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V>
        sortByValue(final Map<K, V> map) {
        List<Map.Entry<K, V>> list =
            new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(final Map.Entry<K, V> o1,
                    final Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Show statistical information of Similarities.
     * @param domain_graph
     * @param prune_threshold
     */
    private void showSimilaritiesInfo(final Graph<Domain> domain_graph,
            final Double prune_threshold) {
        ArrayList<Double> similarities = new ArrayList<Double>();
        for (Domain dom : domain_graph.getNodes()) {
            NeighborList neighbors = domain_graph.getNeighbors(dom);
            for (Neighbor<Domain> neighbor : neighbors) {
                similarities.add(neighbor.similarity);
            }
        }
        double mean = getMean(similarities);
        double variance = getVariance(similarities);
        double z = getZ(similarities, prune_threshold);
        System.out.println("Similarities : ");
        System.out.println("    Mean = " + mean);
        System.out.println("    Variance = " + variance);
        System.out.println("    z = " + z);
    }

    /**
     * Show statistical information of Cluster Sizes.
     * @param clusters
     * @param max_cluster_size
     */
    private void showClusterSizeInfo(final ArrayList<Graph<Domain>> clusters,
            final int max_cluster_size) {
        ArrayList<Double> cluster_sizes = new ArrayList<Double>();
        for (Graph<Domain> subgraph : clusters) {
            cluster_sizes.add((double) subgraph.size());
        }
        double mean = getMean(cluster_sizes);
        double variance = getVariance(cluster_sizes);
        double z = getZ(cluster_sizes, (double) max_cluster_size);
        System.out.println("Cluster Size : ");
        System.out.println("    Mean = " + mean);
        System.out.println("    Variance = " + variance);
        System.out.println("    z = " + z);
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
     * Compute the variance of an ArrayList<Double>.
     * @param list
     * @return variance
     */
    private double getVariance(final ArrayList<Double> list) {
        double mean = getMean(list);
        double sum = 0.0;
        for (double i :list) {
            sum += (i - mean) * (i - mean);
        }
        return sum / list.size();
    }

    /**
     * Compute the z score of a value.
     * @param list
     * @param value
     * @return z
     */
    private double getZ(final ArrayList<Double> list, final Double value) {
        double mean = getMean(list);
        double variance = getVariance(list);
        return (value - mean) / Math.sqrt(variance);
    }

    /**
     * White List unwanted domains.
     * @param domain_graph
     * @return domain_graph
     */
    private Graph<Domain> whiteListing(final Graph<Domain> domain_graph) {
        Graph<Domain> domain_graph_new = domain_graph;
        LinkedList<String> whitelist = new LinkedList<String>();
        try {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(hosts_file, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                whitelist.add(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(RequestHandler.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        for (Domain dom : domain_graph_new.getNodes()) {
            if (whitelist.contains(dom.toString())) {
                domain_graph_new.fastRemove(dom);
            }
        }
        return domain_graph_new;
    }

    static <U> void remove(Graph<U> graph, U node) {
        HashMap<U, NeighborList> map = graph.getHashMap();

        // Supprime le node
        map.remove(node);

        // Maintenant il faut supprimer les edges invalides
        // sinon on risque des erreurs "NullPointerException"
        for (Map.Entry<U, NeighborList> entry : map.entrySet()) {
            NeighborList neighborlist = entry.getValue();

            // On parcourt la liste, et en même temps on supprime des
            // éléments de la  liste => for() ne peut pas être utilisé!
            // http://stackoverflow.com/questions/223918/
            Iterator<Neighbor> iterator = neighborlist.iterator();
            while (iterator.hasNext()) {
                Neighbor<U> neighbor = iterator.next();
                if (neighbor.node.equals(node)) {
                    iterator.remove();
                }
            }
        }

    }
}
