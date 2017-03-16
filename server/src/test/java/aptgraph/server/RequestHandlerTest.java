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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
        System.out.println("test");

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
        System.out.println("dummy");

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
                new double[]{0.8, 0.2}, 0.0, 0.0, true, true, true, true, "",
                new double[]{0.45, 0.45, 0.1});
    }

    /**
     * Test of the integrity of domains during fusion of features
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityFusionFeatures()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : fusion of features");

        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});

        // Test presence of all the domains
        for (Graph<Request> graph_temp : graphs) {
            System.out.println("Before fusion = " + graph_temp.getNodes());
            System.out.println("After fusion = " + merged_graph.getNodes());
            for (Request req : graph_temp.getNodes()) {
                assertTrue(merged_graph.containsKey(req));
            }
        }

        // Test the lost of neighbors
        for (Graph<Request> graph_temp : graphs) {
            for (Request req : graph_temp.getNodes()) {
                NeighborList nl_temp = graph_temp.getNeighbors(req);
                NeighborList nl_merged = merged_graph.getNeighbors(req);
                for (Neighbor<Request> nb : nl_temp) {
                    if (nb.similarity != 0) {
                        assertTrue(nl_merged.containsNode(nb.node));
                    }
                }
            }
        }
    }

    /**
     * Test of the integrity of domains during computation of domains
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityComputeDomains()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : computation of domains");
        
        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);

        // Test
        System.out.println("Before computation = " + merged_graph.getNodes());
        System.out.println("After computation = " + domains.keySet());
        for (Request req : merged_graph.getNodes()) {
            assertTrue(domains.get(req.getDomain()).contains(req));
        }
    }

    /**
     * Test of the integrity of domains during computation of domain similarity
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainSimilarity()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : computation of domain similarity");
        
        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);
        Graph<Domain> domain_graph =
                handler.computeSimilarityDomain(merged_graph, domains);

        // Test presence of all domains
        System.out.println("Before computation = " + domains.keySet());
        System.out.println("After computation = " + domain_graph.getNodes());
        for (Domain dom : domains.values()) {
            assertTrue(domain_graph.containsKey(dom));
        }

        // Test the lost of neighbors
        for (Request req : merged_graph.getNodes()) {
            NeighborList nl_req = merged_graph.getNeighbors(req);
            NeighborList nl_dom =
                    domain_graph.getNeighbors(
                            domains.get(req.getDomain()));
            for (Neighbor<Request> nb : nl_req) {
                if(nb.similarity != 0
                        && !nb.node.getDomain().equals(req.getDomain())) {
                    assertTrue(nl_dom.containsNode(
                            domains.get(nb.node.getDomain())));
                }
            } 
        }
    }

    /**
     * Test of the integrity of domains during pruning
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainPruning()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : pruning");

        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);
        Graph<Domain> domain_graph =
                handler.computeSimilarityDomain(merged_graph, domains);
        Graph<Domain> domain_graph_old = domain_graph;
        // Test both method to prune
        handler.doPruning(domain_graph, (long) 0, true, 0.0);
        handler.doPruning(domain_graph, (long) 0, false, 0.5);

        // Test
        System.out.println("Before pruning = " + domain_graph_old.getNodes());
        System.out.println("After pruning = " + domain_graph.getNodes());
        for (Domain dom : domain_graph_old.getNodes()) {
            assertTrue(domain_graph.containsKey(dom));
        }
    }

    /**
     * Test of the integrity of domains during clustering
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainCluster()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : clusering");
        
        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);
        Graph<Domain> domain_graph =
                handler.computeSimilarityDomain(merged_graph, domains);
        handler.doPruning(domain_graph, (long) 0, false, 0.5); 
        ArrayList<Graph<Domain>> clusters = domain_graph.connectedComponents();

        // Test
        System.out.println("After prune = " + domain_graph.getNodes());
        System.out.println("Clusters = " + clusters);
        boolean found_node = false;
        for (Domain dom : domain_graph.getNodes()) {
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
     * Test the effectiveness of the suppression of a node in a graph.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testRemove()
            throws IOException, ClassNotFoundException {
        System.out.println("Test : remove");
        
        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.3});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);
        Graph<Domain> domain_graph =
                handler.computeSimilarityDomain(merged_graph, domains);
        
        // Test 
        Domain first_node = domain_graph.first();
        LinkedList<Domain> nodes = new LinkedList<Domain>();
        nodes.add(first_node);
        RequestHandler.remove(domain_graph, nodes);
        assertFalse(domain_graph.containsKey(first_node));
    }

    /**
     * Test the effectiveness of the white listing of some node.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testWhiteListing()
            throws IOException, ClassNotFoundException {
        System.out.println("Test : white listing");
        
        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths
                        .get("src/test/resources/dummyDir_whitelist"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);
        Graph<Request> merged_graph =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.3});
        HashMap<String, Domain> domains =
                handler.computeDomainNodes(merged_graph);
        Graph<Domain> domain_graph =
                handler.computeSimilarityDomain(merged_graph, domains);
        ArrayList<Graph<Domain>> clusters = domain_graph.connectedComponents();
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < 1000) {
                filtered.add(subgraph);
            }
        }
        
        // Test 
        LinkedList<Graph<Domain>> filtered_new = filtered;
        Graph<Domain> domain_graph_new = filtered_new.getFirst();
        Domain domain_node_1 = new Domain();
        Domain domain_node_2 = new Domain();
        Domain domain_node_3 = new Domain();
        for (Domain dom : domain_graph_new.getNodes()) {
            if (dom.toString().equals("stats.g.doubleclick.net")) {
              domain_node_1 = dom;
            }
            if (dom.toString().equals("ad.doubleclick.net")) {
                domain_node_2 = dom;
            }
            if (dom.toString().equals("google-analytics.com")) {
                domain_node_3 = dom;
            }
        }

        filtered_new = handler.whiteListing(filtered, "google-analytics.com");
        Graph<Domain> domain_graph_new_bis = filtered_new.getFirst();

        assertFalse(domain_graph_new_bis.containsKey(domain_node_1));
        assertFalse(domain_graph_new_bis.containsKey(domain_node_2));
        assertFalse(domain_graph_new_bis.containsKey(domain_node_3));
    }

    /**
     * Test the good transmission of the weights.
     */
    public void testWeightsFeatures() {
        System.out.println("Fusion : test weights");

        // Creation of the data
        RequestHandler handler =
                new RequestHandler(Paths.get("src/test/resources/dummyDir"));
        String user = handler.getUsers().get(0);
        LinkedList<Graph<Request>> graphs = handler.getUserGraphs(user);

        // Test time weight
        Graph<Request> time_graph = graphs.get(0);
        Graph<Request> merged_graph_time =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{1.0, 0.0, 0.0});
        for (Request req : merged_graph_time.getNodes()) {
            assertTrue(time_graph.containsKey(req));
            NeighborList nb_list = time_graph.getNeighbors(req);
            for (Neighbor nb : merged_graph_time.getNeighbors(req)) {
                assertTrue(nb_list.contains(nb));   
            }
        }

        // Test domain weight
        Graph<Request> domain_graph = graphs.get(1);
        Graph<Request> merged_graph_domain =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.0, 1.0, 0.0});
        for (Request req : merged_graph_domain.getNodes()) {
            assertTrue(domain_graph.containsKey(req));
            NeighborList nb_list = domain_graph.getNeighbors(req);
            for (Neighbor nb : merged_graph_domain.getNeighbors(req)) {
                assertTrue(nb_list.contains(nb));
            }
        }

        // Test URL weight (if activated)
        if (graphs.size() == 3) {
            Graph<Request> url_graph = graphs.get(2);
            Graph<Request> merged_graph_url =
                handler.computeFusionFeatures(graphs,
                        new double[]{0.8, 0.2}, new double[]{0.0, 0.0, 1.0});
            for (Request req : merged_graph_url.getNodes()) {
                assertTrue(url_graph.containsKey(req));
                NeighborList nb_list = url_graph.getNeighbors(req);
                for (Neighbor nb : merged_graph_url.getNeighbors(req)) {
                    assertTrue(nb_list.contains(nb));
                }
            }
        }
    }
}
