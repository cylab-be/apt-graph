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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final HashMap<String,
            LinkedList<Graph<Request>>> user_graphs;

    RequestHandler(final HashMap<String,
            LinkedList<Graph<Request>>> user_graphs) {
        this.user_graphs = user_graphs;
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
     * @param user
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
        for (HashMap.Entry<String, LinkedList<Graph<Request>>> entry_set
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

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        // From Graph of Requests to HashMap of Domains
        // (it contains every requests of a specific domain, for each domain)
        HashMap<String, Domain> domains =
                computeDomainGraph(merged_graph);

        // Compute similarity between domains and build domain graph
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

        // Prune & clustering
        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        domain_graph.prune(prune_threshold);

        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        ArrayList<Graph<Domain>> clusters = domain_graph.connectedComponents();

        // Filtering
        // The json-rpc request was probably canceled by the user
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < max_cluster_size) {
                filtered.add(subgraph);
            }
        }

        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;
    }

    private Graph<Request> computeFusionFeatures(
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

    private HashMap<String, Domain> computeDomainGraph(
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
}
