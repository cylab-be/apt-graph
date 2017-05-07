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

import aptgraph.core.Domain;
import aptgraph.core.DomainSimilarity;
import aptgraph.core.Request;
import aptgraph.core.Subnet;
import aptgraph.core.TimeSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Test file for Batch Processor.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
 */
public class BatchProcessorTest extends TestCase {

    /**
     * Test of analyze method, of class BatchProcessor.
     *
     * @throws IOException if the input file is not found
     */
    public final void testAnalyze() throws IOException {
        System.out.println("Test batch server with 1000 reqs");
        Path temp_dir = Files.createTempDirectory("tempdir");

        BatchProcessor processor = new BatchProcessor();
        processor.analyze(20,
                getClass().getResourceAsStream("/1000_http_requests.txt"),
                temp_dir, "squid", true, true);

    }

    /**
     * Test that a graph is correctly (de)serialized.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public final void testSerialize()
            throws IOException, ClassNotFoundException {

        System.out.println("Test serialization with 1000 reqs");
        Path temp_dir = Files.createTempDirectory("tempdir");

        int k = 20;
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt"),
                        "squid"));
        ArrayList<String> user_list = new ArrayList<String>();
        for (String user : user_requests.keySet()) {
            user_list.add(user);
        }
        LinkedList<Graph<Domain>> original_user_graphs
                = processor.computeUserGraphs(k, "test_user",
                        user_requests.values().iterator().next(), true);
        processor.saveGraphs(temp_dir, "test_user", original_user_graphs);
        processor.saveK(temp_dir, k);
        processor.saveSubnet(temp_dir, user_list);
        processor.saveUsers(temp_dir, user_list);

        File temp_file_1
                = new File(temp_dir.toString(), "test_user" + ".ser");
        ObjectInputStream ois_1 = new ObjectInputStream(
                new FileInputStream(temp_file_1));
        LinkedList<Graph<Request>> deserialized_user_graphs
                = (LinkedList<Graph<Request>>) ois_1.readObject();
        assertEquals(original_user_graphs, deserialized_user_graphs);

        File temp_file_2
                = new File(temp_dir.toString(), "users.ser");
        ObjectInputStream ois_2 = new ObjectInputStream(
                new FileInputStream(temp_file_2));
        ArrayList<String> deserialized_user_list
                = (ArrayList<String>) ois_2.readObject();
        assertEquals(Subnet.sortIPs(user_list), deserialized_user_list);

        File temp_file_3
                = new File(temp_dir.toString(), "subnets.ser");
        ObjectInputStream ois_3 = new ObjectInputStream(
                new FileInputStream(temp_file_3));
        ArrayList<String> deserialized_subnet_list
                = (ArrayList<String>) ois_3.readObject();
        assertEquals(Subnet.getAllSubnets(user_list), deserialized_subnet_list);

        File temp_file_4
                = new File(temp_dir.toString(), "k.ser");
        ObjectInputStream ois_4 = new ObjectInputStream(
                new FileInputStream(temp_file_4));
        int deserialized_k = ois_4.readInt();
        assertEquals(k, deserialized_k);
    }

    /**
     * Test graph building (verify k and duplicated neighbors).
     *
     * @throws IOException
     */
    public final void testGraph() throws IOException {

        int k = 5;
        int trials = 10;

        for (int i = 0; i < trials; i++) {

            BatchProcessor processor = new BatchProcessor();
            HashMap<String, LinkedList<Request>> user_requests
                    = processor.computeUserLog(processor.parseFile(getClass()
                            .getResourceAsStream("/12_3_site_3_time_1_APT_ad.txt"),
                            "squid"));

            for (Map.Entry<String, LinkedList<Request>> entry
                    : user_requests.entrySet()) {
                LinkedList<Request> requests = entry.getValue();
                Graph<Request> user_graph
                        = processor.computeRequestGraph(requests,
                                k, new TimeSimilarity());

                for (Request req : user_graph.getNodes()) {
                    // Check k
                    NeighborList neighbors = user_graph.getNeighbors(req);
                    assertEquals(k, neighbors.size());

                    // Check duplicated neighbors
                    LinkedList<Neighbor<Request>> nb_list
                            = new LinkedList<Neighbor<Request>>();
                    for (Neighbor<Request> nb : neighbors) {
                        assertFalse(nb_list.contains(nb));
                        nb_list.add(nb);
                    }
                }
            }
        }
    }

    /**
     * Test integrity of the computation of the user logs.
     *
     * @throws IOException
     */
    public void testComputeUserLog()
            throws IOException {
        System.out.println("Test of the computation of users log.");
        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        LinkedList<Request> requests = processor.parseFile(getClass()
                .getResourceAsStream("/1000_http_requests.txt"),
                "squid");
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(requests);
        // Test
        int total_requests = 0;
        for (LinkedList list : user_requests.values()) {
            total_requests += list.size();
        }
        assertTrue(total_requests == requests.size());
        for (Request req : requests) {
            assertTrue(user_requests.get(req.getClient()).contains(req));
        }
    }

    /**
     * Test the computation of domains.
     *
     * @throws IOException
     */
    public void testComputeDomain()
            throws IOException {
        System.out.println("Test of the computation of domains");
        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/domain_test.txt"), "squid"));
        LinkedList<Request> requests = user_requests.get("127.0.0.1");
        for (Request req : requests) {
            String dom = req.getDomain();
            String[] dom_split = dom.split("[.]");
            assertTrue(dom_split[0].equals("domain"));
        }
    }

    /**
     * Test of the integrity of domains during computation of domains
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityComputeDomains()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : computation of domains");

        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt"),
                        "squid"));
        LinkedList<Request> requests = user_requests.get("198.36.158.8");
        HashMap<String, Domain> domains
                = processor.computeDomainNodes(requests);

        // Test
        int total_requests = 0;
        for (Domain dom : domains.values()) {
            total_requests += dom.size();
        }
        assertTrue(total_requests == requests.size());
        for (Request req : requests) {
            assertTrue(domains.get(req.getDomain()).contains(req));
        }
    }

    /**
     * Test the integrity of the computation of a graph of requests.
     *
     * @throws IOException
     */
    public void testComputeRequestGraph()
            throws IOException {
        System.out.println("Integrity : computation of graph of requests");

        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt"),
                        "squid"));
        LinkedList<Request> requests = user_requests.get("198.36.158.8");
        Graph<Request> time_graph = processor.computeRequestGraph(
                requests, 20, new TimeSimilarity());

        // Test presence of all requests
        int total_requests = 0;
        for (Request req : time_graph.getNodes()) {
            total_requests += 1;
            assertTrue(requests.contains(req));
        }
        assertTrue(total_requests == requests.size());
    }

    /**
     * Test the resistance of ThreadedNNDescent
     * algorithm to simultaneous requests.
     * @throws IOException 
     */
    public void testSimultaneousRequests()
            throws IOException {
        System.out.println("Test simultaneous requests");
        
        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests =
                processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/simultaneous.txt"),
                        "squid"));
        LinkedList<Request> requests = user_requests.get("127.0.0.1");
        ThreadedNNDescent<Request> nndes = new ThreadedNNDescent<Request>();
        nndes.setSimilarity(new TimeSimilarity());
        nndes.setK(20);
        System.out.println("START with ThreadedNNDescent");
        System.out.println("Wait for the STOP");
        System.out.println("Test fail if it never stops !");
        Graph<Request>  graph = nndes.computeGraph(requests);
        System.out.println("STOP");

        // Test fail because it never stops       
    }

    /**
     * Test selection of children.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testChildrenSelection()
            throws IOException, ClassNotFoundException {
        System.out.println("Test Follow Requests");

        BatchProcessor processor = new BatchProcessor();

        // Create Data
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/simple.txt"), "squid"));
        LinkedList<Request> requests_all = user_requests.get("127.0.0.1");
        Graph<Request> time_graph
                = processor.computeRequestGraph(requests_all, 20,
                        new DomainSimilarity());
        Graph<Request> time_graph_old = new Graph<Request>(time_graph);
        time_graph = processor.childrenSelection(time_graph);

        // Test the children
        for (Request req : time_graph_old.getNodes()) {
            NeighborList nl = time_graph_old.getNeighbors(req);
            for (Neighbor<Request> nb : nl) {
                if (nb.getNode().getTime() >= req.getTime()) {
                    assertTrue(time_graph.getNeighbors(req).contains(nb));
                } else {
                    assertFalse(time_graph.getNeighbors(req).contains(nb));
                }
            }
        }
    }

    /**
     * Test of the integrity of domains during computation of domain similarity.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testIntegrityDomainSimilarity()
            throws IOException, ClassNotFoundException {
        System.out.println("Integrity : computation of domain similarity");

        // Creation of the data
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests
                = processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt"),
                        "squid"));
        for (String user : user_requests.keySet()) {
            LinkedList<Request> requests = user_requests.get(user);
            Graph<Request> time_graph = processor.computeRequestGraph(
                    requests, 20, new TimeSimilarity());
            // Create the domain nodes
            // (it contains every requests of a specific domain, for each domain)
            HashMap<String, Domain> domains
                    = processor.computeDomainNodes(requests);
            // Compute similarity between domains and build domain graph
            Graph<Domain> time_domain_graph
                    = processor.computeSimilarityDomain(time_graph, domains);

            HashMap<String, Domain> all_domains_merged
                    = new HashMap<String, Domain>();
            for (Domain dom : time_domain_graph.getNodes()) {
                all_domains_merged.put(dom.getName(), dom);
            }

            // Test presence of all the domains and requests after feature fusion
            for (Domain dom_1 : domains.values()) {
                for (Domain dom_2 : all_domains_merged.values()) {
                    if (dom_1.getName().equals(dom_2.getName())) {
                        assertTrue(dom_1.deepEquals(dom_2));
                    }
                }
            }

            // Test the lost of neighbors
            for (Request req : time_graph.getNodes()) {
                NeighborList nl_req = time_graph.getNeighbors(req);
                NeighborList nl_dom
                        = time_domain_graph.getNeighbors(
                                domains.get(req.getDomain()));
                for (Neighbor<Request> nb : nl_req) {
                    if (nb.getSimilarity() != 0
                            && !nb.getNode().getDomain().equals(req.getDomain())) {
                        assertTrue(nl_dom.containsNode(
                                domains.get(nb.getNode().getDomain())));
                    }
                }
            }

            // Test the similarities
            for (Domain dom_1 : time_domain_graph.getNodes()) {
                NeighborList nl_dom_1 = time_domain_graph.getNeighbors(dom_1);
                for (Neighbor<Domain> dom_2 : nl_dom_1) {
                    double similarity_temp = 0.0;
                    for (Request req_1 : time_graph.getNodes()) {
                        if (req_1.getDomain().equals(dom_1.getName())) {
                            NeighborList nl_req_1 = time_graph.getNeighbors(req_1);
                            for (Neighbor<Request> req_2 : nl_req_1) {
                                if (req_2.getNode().getDomain().equals(dom_2.getNode().getName())) {
                                    similarity_temp += req_2.getSimilarity();
                                }
                            }
                        }
                    }
                    assertTrue(Math.abs(dom_2.getSimilarity() - similarity_temp)
                            <= 1E-10);
                }
            }
        }
    }
}
