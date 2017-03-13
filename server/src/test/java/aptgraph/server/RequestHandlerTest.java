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
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);
        Graph<Domain> merged_graph =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});

        // Test
        boolean indicator = false;
        for (Graph<Domain> graph_temp : graphs) {
            System.out.println("Before fusion = " + graph_temp.getNodes());
            System.out.println("After fusion = " + merged_graph.getNodes());
            for (Domain dom : graph_temp.getNodes()) {
                indicator = merged_graph.containsKey(dom);
                if (!indicator) {
                    System.out.println("OUT !");
                    break;
                }
            }
        }

        assertTrue(indicator);
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);
        Graph<Domain> merged_graph =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        Graph<Domain> merged_graph_old = merged_graph;
        merged_graph.prune(0.5);

        // Test
        System.out.println("Before pruning = " + merged_graph_old.getNodes());
        System.out.println("After pruning = " + merged_graph.getNodes());
        boolean indicator = false;
        for (Domain dom : merged_graph_old.getNodes()) {
            indicator = merged_graph.containsKey(dom);
            if (!indicator) {
                System.out.println("FAIL !");
                break;
            }
        }

        assertTrue(indicator);
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);
        Graph<Domain> merged_graph =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.1, 0.2});
        merged_graph.prune(0.5);
        ArrayList<Graph<Domain>> clusters = merged_graph.connectedComponents();

        // Test
        System.out.println("After prune = " + merged_graph.getNodes());
        System.out.println("Clusters = " + clusters);
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);
        Graph<Domain> merged_graph =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.3});
        
        // Test 
        Domain first_node = merged_graph.first();
        LinkedList<Domain> nodes = new LinkedList<Domain>();
        nodes.add(first_node);
        RequestHandler.remove(merged_graph, nodes);
        assertFalse(merged_graph.containsKey(first_node));
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);
        Graph<Domain> merged_graph =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.7, 0.3});
        ArrayList<Graph<Domain>> clusters = merged_graph.connectedComponents();
        LinkedList<Graph<Domain>> filtered = new LinkedList<Graph<Domain>>();
        for (Graph<Domain> subgraph : clusters) {
            if (subgraph.size() < 1000) {
                filtered.add(subgraph);
            }
        }
        
        // Test 
        Graph<Domain> domain_graph = filtered.getFirst();
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

        System.out.println("Before whitelisting = " + filtered);
        filtered = handler.whiteListing(filtered, "ss.symcd.com");
        System.out.println("After whitelisting = " + filtered);
        Graph<Domain> domain_graph_new = filtered.getFirst();

        assertFalse(domain_graph_new.containsKey(domain_node_1));
        assertFalse(domain_graph_new.containsKey(domain_node_2));
        assertFalse(domain_graph_new.containsKey(domain_node_3));
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
        LinkedList<Graph<Domain>> graphs = handler.getUserGraphs(user);

        // Test time weight
        Graph<Domain> time_graph = graphs.get(0);
        Graph<Domain> merged_graph_time =
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{1.0, 0.0, 0.0});
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
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.0, 1.0, 0.0});
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
                handler.computeFusionFeatures(20, graphs,
                        new double[]{0.8, 0.2}, new double[]{0.0, 0.0, 1.0});
            for (Domain dom : merged_graph_url.getNodes()) {
                assertTrue(url_graph.containsKey(dom));
                NeighborList nb_list = url_graph.getNeighbors(dom);
                for (Neighbor nb : merged_graph_url.getNeighbors(dom)) {
                    assertTrue(nb_list.contains(nb));
                }
            }
        }
    }
}
