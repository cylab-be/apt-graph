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
import aptgraph.core.Request;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import static junit.framework.Assert.assertTrue;
import junit.framework.TestCase;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandlerTest extends TestCase {

    /**
     * Test of test method, of class RequestHandler.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testTest() throws IOException, ClassNotFoundException {
        System.out.println("\ntest");

        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        handler.test();
    }

    /**
     * Test of dummy method, of class RequestHandler.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testDummy() throws IOException, ClassNotFoundException {
        System.out.println("\ndummy");

        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        handler.dummy();
    }

    /**
     * Test of analyze method, of class RequestHandler.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testAnalyze() throws IOException, ClassNotFoundException {
        System.out.println("analyze");

        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        handler.getUsers();
        handler.analyze("253.115.106.54", new double[]{0.7, 0.1, 0.2},
                new double[]{0.8, 0.2}, 0.0, 0.0, true, true, true, "",
                new double[]{0.45, 0.45, 0.1}, true);
    }

    /**
     * Test the integrity of the all_domains HashMap.
     */
    public void testIntegrityAllDomains() {
        System.out.println("\nIntegrity : all_domains");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        HashMap<String, HashMap<String, Domain>> all_domains
                = handler.getMemory().getAllDomains();

        // Test
        for (String user : handler.getMemory().getAllUsersList()) {
            LinkedList<Graph<Domain>> graphs = FileManager.getUserGraphs(
                    input_dir, user);
            for (Graph<Domain> graph : graphs) {
                for (Domain dom : graph.getNodes()) {
                    assertTrue(all_domains.get("byUsers")
                            .get(user + ":" + dom).equals(dom));
                    for (Request req : dom) {
                        assertTrue(all_domains.get("all")
                                .get(dom.getName()).contains(req));
                    }
                }
            }
        }
        
    }

    /**
     * Test of the integrity of domains during fusion of graphs.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityFusionFeatures()
            throws IOException, ClassNotFoundException {
        System.out.println("\nIntegrity : fusion of features");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        double[] weights = {0.7, 0.1, 0.2};
        for (String user : handler.getMemory().getAllUsersList()) {   
            LinkedList<Graph<Domain>> graphs
                    = handler.getMemory().getUsersGraphs().get(user);
            Graph<Domain> merged_graph =
                    handler.computeFusionGraphs(graphs, user, weights,
                            new double[]{0.8, 0.2}, "byUsers");

            HashMap<String, Domain> all_domains_merged
                    = new HashMap<String, Domain>();
            for (Domain dom : merged_graph.getNodes()) {
                all_domains_merged.put(dom.getName(), dom);
            }

            // Test presence of all the domains and requests after feature fusion
            for (Map.Entry<String, Domain> entry : handler.getMemory()
                    .getAllDomains().get("byUsers").entrySet()) {
                String key = entry.getKey();
                Domain dom_1 = entry.getValue();
                if (key.startsWith(user)) {
                    for (Domain dom_2 : all_domains_merged.values()) {
                        if (dom_1.getName().equals(dom_2.getName())) {
                            assertTrue(dom_1.equals(dom_2));
                        }
                    }
                }
            }

            // Test the lost of neighbors (domains and requests)
            for (Graph<Domain> graph_temp : graphs) {
                for (Domain dom : graph_temp.getNodes()) {
                    NeighborList nl_temp = graph_temp.getNeighbors(dom);
                    NeighborList nl_merged = merged_graph.getNeighbors(dom);
                    for (Neighbor<Domain> nb : nl_temp) {
                        if (nb.similarity != 0) {
                            assertTrue(nl_merged.containsNode(nb.node));
                            for (Neighbor<Domain> nb_merged : nl_merged) {
                                if (nb_merged.node.getName()
                                        .equals(nb.node.getName())) {
                                    assertTrue(nb.node.equals(nb_merged.node));
                                }
                            }
                        }
                    }
                }
            }

            // Test the similarities
            for (Domain dom_11 : merged_graph.getNodes()) {
                NeighborList nl_dom_11 = merged_graph.getNeighbors(dom_11);
                for (Neighbor<Domain> dom_12 : nl_dom_11) {
                    double similarity_temp = 0.0;
                    for (int i = 0; i < graphs.size(); i++) {
                        Graph<Domain> feature_graph = graphs.get(i);
                        double feature_weight = weights[i];
                        for (Domain dom_21 : feature_graph.getNodes()) {
                            if (dom_21.equals(dom_11)) {
                                NeighborList nl_dom_21 = feature_graph
                                        .getNeighbors(dom_21);
                                for (Neighbor<Domain> dom_22 : nl_dom_21) {
                                    if (dom_22.node.equals(dom_12.node)) {
                                        similarity_temp += feature_weight
                                                * dom_22.similarity;
                                    }
                                }
                            }
                        }
                    }
                    assertTrue(dom_12.similarity == similarity_temp);
                }
            }
        }
    }

    /**
     * Test the good transmission of the weights.
     */
    public void testWeightsFeatures() {
        System.out.println("\nFusion : test weights");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setUser(handler.getMemory().getUsersList().get(0));
        LinkedList<Graph<Domain>> graphs = handler.getMemory().getUsersGraphs()
                .get(handler.getMemory().getUser());

        // Test time weight
        Graph<Domain> time_graph = graphs.get(0);
        Graph<Domain> merged_graph_time =
                handler.computeFusionGraphs(graphs, handler.getMemory().getUser(),
                        new double[]{1.0, 0.0, 0.0}, new double[]{0.8, 0.2},
                        "byUsers");
        for (Domain dom : merged_graph_time.getNodes()) {
            assertTrue(time_graph.containsKey(dom));
            NeighborList nb_list = time_graph.getNeighbors(dom);
            for (Neighbor nb : merged_graph_time.getNeighbors(dom)) {
                assertTrue(nb_list.contains(nb));   
            }
        }

        // Test domain weight
        Graph<Domain> domain_graph = graphs.get(1);
        Graph<Domain> merged_graph_domain =
                handler.computeFusionGraphs(graphs, handler.getMemory().getUser(),
                        new double[]{0.0, 1.0, 0.0}, new double[]{0.8, 0.2},
                        "byUsers");
        for (Domain dom : merged_graph_domain.getNodes()) {
            assertTrue(domain_graph.containsKey(dom));
            NeighborList nb_list = domain_graph.getNeighbors(dom);
            for (Neighbor nb : merged_graph_domain.getNeighbors(dom)) {
                assertTrue(nb_list.contains(nb));
            }
        }

        // Test URL weight (if activated)
        if (graphs.size() == 3) {
            Graph<Domain> url_graph = graphs.get(2);
            Graph<Domain> merged_graph_url =
                handler.computeFusionGraphs(graphs, handler.getMemory().getUser(),
                        new double[]{0.0, 0.0, 1.0}, new double[]{0.8, 0.2},
                        "byUsers");
            for (Domain dom : merged_graph_url.getNodes()) {
                assertTrue(url_graph.containsKey(dom));
                NeighborList nb_list = url_graph.getNeighbors(dom);
                for (Neighbor nb : merged_graph_url.getNeighbors(dom)) {
                    assertTrue(nb_list.contains(nb));
                }
            }
        }
    }

    /**
     * Test of the integrity of domains during fusion of users.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public void testIntegrityFusionUsers() 
            throws IOException, ClassNotFoundException {
        System.out.println("\nIntegrity : fusion of users");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();

        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = handler.computeFusionGraphs(merged_graph_users, "",
                        users_weights, new double[] {0.0}, "all");

        // Test presence of all the domains and requests after users fusion
        for (Domain dom_1 : handler.getMemory().getAllDomains().get("all").values()) {
            for (Domain dom_2 : merged_graph.getNodes()) {
                if (dom_1.getName().equals(dom_2.getName())) {
                    assertTrue(dom_1.equals(dom_2));
                }
            }
        }

        // Test the lost of neighbors
        for (Graph<Domain> graph_temp : merged_graph_users) {
            String user_temp = graph_temp.getNodes().iterator()
                                .next().element().getClient();
            for (Domain dom : graph_temp.getNodes()) {
                NeighborList nl_temp = graph_temp.getNeighbors(dom);
                NeighborList nl_merged = merged_graph.getNeighbors(
                    handler.getMemory().getAllDomains().get("all").get(dom.getName()));
                for (Neighbor<Domain> nb : nl_temp) {
                    if (nb.similarity != 0) {
                        assertTrue(nl_merged.containsNode(
                                handler.getMemory().getAllDomains()
                                .get("all").get(nb.node.getName())));
                        for (Neighbor<Domain> nb_merged : nl_merged) {
                            if (nb_merged.node.getName()
                                    .equals(nb.node.getName())) {
                                assertTrue(nb.node.equals(
                                        handler.getMemory().getAllDomains()
                                        .get("byUsers").get(user_temp + ":"
                                                + nb_merged.node.getName())));
                            }
                        }
                    }
                }
            }
        }

        // Test the similarities
        for (Domain dom_11 : merged_graph.getNodes()) {
            NeighborList nl_dom_11 = merged_graph.getNeighbors(dom_11);
            for (Neighbor<Domain> dom_12 : nl_dom_11) {
                double similarity_temp = 0.0;
                for (int i = 0; i < merged_graph_users.size(); i++) {
                    Graph<Domain> user_graph = merged_graph_users.get(i);
                    String user_temp = user_graph.getNodes().iterator()
                                .next().element().getClient();
                    double feature_weight = users_weights[i];
                    for (Domain dom_21 : user_graph.getNodes()) {
                        if (dom_21.equals(handler.getMemory().getAllDomains()
                                .get("byUsers").get(user_temp
                                        + ":" + dom_11.getName()))) {
                            NeighborList nl_dom_21 = user_graph
                                    .getNeighbors(dom_21);
                            for (Neighbor<Domain> dom_22 : nl_dom_21) {
                                if (dom_22.node.equals(handler.getMemory()
                                        .getAllDomains().get("byUsers")
                                        .get(user_temp + ":"
                                                + dom_12.node.getName()))) {
                                    similarity_temp += feature_weight
                                            * dom_22.similarity;
                                }
                            }
                        }
                    }
                }
                assertTrue(dom_12.similarity == similarity_temp);
            }
        }
    }

    /**
     * Test of the integrity of the computation of individual graphs.
     */
    public void testIntegrityUsersGraphs() {
        System.out.println("\nIntegrity : users graphs");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();

        // Test
        assertTrue(handler.getMemory().getUsersList().size() ==
                merged_graph_users.size());
    }

    /**
     * Test of the integrity of domains during pruning
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainPruning()
            throws IOException, ClassNotFoundException {
        System.out.println("\nIntegrity : pruning");

        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getMemory().setStdout("");
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();

        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        handler.getMemory().setMergedGraph(handler.computeFusionGraphs(
                merged_graph_users, "", users_weights, new double[] {0.0}, "all"));
        ArrayList<Double> similarities = handler.listSimilarities();
        Graph<Domain> merged_graph_1 = new Graph(handler.getMemory().getMergedGraph());
        Graph<Domain> merged_graph_2 = new Graph(handler.getMemory().getMergedGraph());
        assertEquals(merged_graph_1, handler.getMemory().getMergedGraph());
        assertEquals(merged_graph_2, handler.getMemory().getMergedGraph());

        // Test both method to prune
        handler.getMemory().setMeanVarSimilarities(Utility.getMeanVariance(similarities));
        double prune_threshold = 0.4;
        handler.getMemory().setPruningThresholdTemp(prune_threshold);
        handler.getMemory().setPruneZBool(false);
        merged_graph_1 = handler.doPruning(merged_graph_1, (long) 0);
        assertFalse(merged_graph_1.equals(handler.getMemory().getMergedGraph()));
        handler.getMemory().setPruningThresholdTemp(0.0);
        handler.getMemory().setPruneZBool(true);
        merged_graph_2 = handler.doPruning(merged_graph_2, (long) 0);
        assertFalse(handler.getMemory().getMergedGraph().equals(merged_graph_2));
        assertFalse(merged_graph_1.equals(merged_graph_2));

        // Test presence of all domains
        // System.out.println("Before pruning = " + merged_graph.getNodes());
        // System.out.println("After pruning (1) = " + merged_graph_1.getNodes());
        // System.out.println("After pruning (2) = " + merged_graph_2.getNodes());
        for (Domain dom : handler.getMemory().getMergedGraph().getNodes()) {
            assertTrue(merged_graph_1.containsKey(dom));
            assertTrue(merged_graph_2.containsKey(dom));
        }

        // Test similarities
        for (Domain dom_11 : handler.getMemory().getMergedGraph().getNodes()) {
            NeighborList nl_dom_11 = handler.getMemory().getMergedGraph()
                    .getNeighbors(dom_11);
            for (Neighbor<Domain> dom_12 : nl_dom_11) {
                // Pruning method 1
                for (Domain dom_21 : merged_graph_1.getNodes()) {
                    if (dom_21.getName().equals(dom_11.getName())) {
                        NeighborList nl_dom_21 = merged_graph_1
                                .getNeighbors(dom_21);
                        if (dom_12.similarity > 0.4) {
                            assertTrue(nl_dom_21.contains(dom_12));
                        } else {
                           assertFalse(nl_dom_21.contains(dom_12));
                        }
                    }
                }
                // Pruning method 2
                for (Domain dom_21 : merged_graph_2.getNodes()) {
                    if (dom_21.getName().equals(dom_11.getName())) {
                        NeighborList nl_dom_21 = merged_graph_2
                                .getNeighbors(dom_21);
                        if (dom_12.similarity >
                                handler.getMemory().getMeanVarSimilarities()[0]) {
                            assertTrue(nl_dom_21.contains(dom_12));
                        } else {
                           assertFalse(nl_dom_21.contains(dom_12));
                        }
                    }
                }
            }
        }
    }

    /**
     * Test of the integrity of domains during clustering.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainCluster()
            throws IOException, ClassNotFoundException {
        System.out.println("\nIntegrity : clusering");
        
        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();
        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = handler.computeFusionGraphs(merged_graph_users, "",
                        users_weights, new double[] {0.0}, "all");
        handler.getMemory().setPruningThresholdTemp(0.5);
        handler.getMemory().setPruneZBool(false);
        merged_graph = handler.doPruning(merged_graph, (long) 0);
        ArrayList<Graph<Domain>> clusters = merged_graph.connectedComponents();

        // Test
        // System.out.println("After prune = " + merged_graph.getNodes());
        // System.out.println("Clusters = " + clusters);
        boolean found_node;
        for (Domain dom : merged_graph.getNodes()) {
            found_node = false;
            for (Graph<Domain> graph : clusters) {
                found_node = graph.containsKey(dom);
                if (found_node) {
                    break;
                }
            }

            assertTrue(found_node);
        }
    }

    /**
     * Test effectiveness of filtering.
     */
    public void testFiltering() {
        System.out.println("\nTest : filtering");
        
        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();
        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = handler.computeFusionGraphs(merged_graph_users, "",
                        users_weights, new double[] {0.0}, "all");
        handler.getMemory().setPruningThresholdTemp(0.5);
        handler.getMemory().setPruneZBool(false);
        merged_graph = handler.doPruning(merged_graph, (long) 0);
        handler.getMemory().setClusters(merged_graph.connectedComponents());

        // Method 1
        handler.getMemory().setMaxClusterSizeTemp(10);
        handler.getMemory().setClusterZBool(false);
        handler.doFiltering(System.currentTimeMillis());
        for (Graph<Domain> graph : handler.getMemory().getFiltered()) {
            assertTrue(graph.size() <= 10);
        }
        // Method 2
        handler.getMemory().setMaxClusterSizeTemp(0);
        handler.getMemory().setClusterZBool(true);
        ArrayList<Double> cluster_sizes = handler.listClusterSizes(
                handler.getMemory().getClusters());
        handler.getMemory().setMeanVarClusters(Utility.getMeanVariance(cluster_sizes));
        handler.getMemory().setStdout("");
        handler.doFiltering(System.currentTimeMillis());
        for (Graph<Domain> graph : handler.getMemory().getFiltered()) {
            assertTrue(graph.size() <= handler.getMemory().getMeanVarClusters()[0]);
        }
    }

    /**
     * Test the effectiveness of the white listing of some node.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testWhiteListing()
            throws IOException, ClassNotFoundException {
        System.out.println("\nTest : white listing");
        
        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir_whitelist");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getMemory().setStdout("");
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();
        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = handler.computeFusionGraphs(merged_graph_users, "",
                        users_weights, new double[] {0.0}, "all");
        handler.getMemory().setClusters(merged_graph.connectedComponents());
        handler.getMemory().setMaxClusterSizeTemp(1000);
        handler.getMemory().setClusterZBool(false);
        handler.doFiltering(System.currentTimeMillis());
        
        // Test 
        Graph<Domain> domain_graph = handler.getMemory().getFiltered().getFirst();
        Domain domain_node_1 = new Domain();
        Domain domain_node_2 = new Domain();
        Domain domain_node_3 = new Domain();
        for (Domain dom : domain_graph.getNodes()) {
            if (dom.toString().equals("stats.g.doubleclick.net")) {
              domain_node_1 = dom;
            }
            if (dom.toString().equals("ad.doubleclick.net")) {
                domain_node_2 = dom;
            }
            if (dom.toString().equals("ss.symcd.com")) {
                domain_node_3 = dom;
            }
        }

        // System.out.println("Before whitelisting = " + filtered);
        Graph<Domain> domain_graph_new_1 = new Graph<Domain>(domain_graph);
        assertTrue(domain_graph_new_1.containsKey(domain_node_1));
        assertTrue(domain_graph_new_1.containsKey(domain_node_2));
        assertTrue(domain_graph_new_1.containsKey(domain_node_3));
        handler.getMemory().setWhiteOngo("ss.symcd.com");
        handler.whiteListing();

        // System.out.println("After whitelisting = " + filtered);
        Graph<Domain> domain_graph_new_2
                = handler.getMemory().getFilteredWhiteListed().getFirst();
        assertFalse(domain_graph_new_2.containsKey(domain_node_1));
        assertFalse(domain_graph_new_2.containsKey(domain_node_2));
        assertFalse(domain_graph_new_2.containsKey(domain_node_3));
    }
}
