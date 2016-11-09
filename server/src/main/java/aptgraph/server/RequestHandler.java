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
import info.debatty.java.graphs.Node;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final LinkedList<Graph<Request>> graphs;

    RequestHandler(final LinkedList<Graph<Request>> graphs) {
        this.graphs = graphs;
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
        Graph<Request> graph = graphs.getFirst();

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
     * @param feature_ordered_weights
     * @param prune_threshold
     * @param feature_weights
     * @param max_cluster_size
     * @return
     */
    public final List<Graph<Domain>> analyze(
            final double[] feature_ordered_weights,
            final double[] feature_weights,
            final double prune_threshold,
            final int max_cluster_size) {

        int k = graphs.getFirst().getK();

        // Feature Fusion
        // Weighted average using parameter feature_weights
        Graph<Request> merged_graph = new Graph<Request>(k);
        for (Node node : graphs.getFirst().getNodes()) {
            HashMap<Node, Double> all_neighbors = new HashMap<Node, Double>();

            for (int i = 0; i < graphs.size(); i++) {
                Graph<Request> feature_graph = graphs.get(i);
                NeighborList feature_neighbors = feature_graph.get(node);

                for (Neighbor feature_neighbor : feature_neighbors) {
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
            for (Entry<Node, Double> entry : all_neighbors.entrySet()) {
                nl.add(new Neighbor(entry.getKey(), entry.getValue()));
            }

            merged_graph.put(node, nl);
        }

        // URL/Domain clustering
        // Associate each domain_name (String) to a Node<Domain>
        HashMap<String, Node<Domain>> domains =
                new HashMap<String, Node<Domain>>();
        for (Node<Request> node : merged_graph.getNodes()) {
            try {
                String domain_name = getDomain(node.value.getUrl());

                Node<Domain> domain_node;
                if (domains.containsKey(domain_name)) {
                    domain_node = domains.get(domain_name);

                } else {
                    domain_node =
                        new Node<Domain>(domain_name, new Domain());
                    domains.put(domain_name, domain_node);
                }

                domain_node.value.add(node);

            } catch (URISyntaxException ex) {
                Logger.getLogger(RequestHandler.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }

        // Compute similarity between domains
        // A domain is (for now) a list of Node<Request>.
        Graph<Domain> domain_graph = new Graph<Domain>(Integer.MAX_VALUE);

        // For each domain
        for (Entry<String, Node<Domain>> domain_entry : domains.entrySet()) {
            String domain_name = domain_entry.getKey();
            Node<Domain> domain_node = domain_entry.getValue();

            HashMap<Node<Domain>, Double> other_domains_sim =
                    new HashMap<Node<Domain>, Double>();

            // For each request in this domain
            for (Node request_node : domain_entry.getValue().value) {

                // Check each neighbor
                for (Neighbor neighbor : merged_graph.get(request_node)) {
                    Request target_request = (Request) neighbor.node.value;
                    try {

                        // Find the corresponding domain name
                        String other_domain_name =
                                getDomain(target_request.getUrl());
                        if (other_domain_name.equals(domain_name)) {
                            continue;
                        }

                        Node<Domain> other_domain =
                                domains.get(other_domain_name);
                        double new_similarity = neighbor.similarity;
                        if (other_domains_sim.containsKey(other_domain)) {
                            new_similarity +=
                                    other_domains_sim.get(other_domain);
                        }

                        other_domains_sim.put(
                                other_domain,
                                new_similarity);
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(RequestHandler.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }
                }
            }

            NeighborList this_domain_neighbors =
                    new NeighborList(Integer.MAX_VALUE);
            for (Entry<Node<Domain>, Double> other_domain_entry
                    : other_domains_sim.entrySet()) {
                this_domain_neighbors.add(new Neighbor(
                        other_domain_entry.getKey(),
                        other_domain_entry.getValue()));
            }

            domain_graph.put(
                    domain_node,
                    this_domain_neighbors);

        }

        // Prune & clustering
        domain_graph.prune(prune_threshold);
        ArrayList<Graph<Domain>> clusters = domain_graph.connectedComponents();

        // Filtering
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < max_cluster_size) {
                filtered.add(subgraph);
            }
        }
        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;
    }

    /**
     * Return the domain name from URL (without wwww.).
     * @param url
     * @return
     * @throws URISyntaxException if url is not correctly formed
     */
    public static String getDomain(final String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain.startsWith("www.")) {
            return domain.substring(4);
        }

        return domain;
    }
}
