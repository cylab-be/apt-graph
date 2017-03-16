package aptgraph.batch;

import aptgraph.core.Domain;
import aptgraph.core.Request;
import aptgraph.core.TimeSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author Thibault Debatty
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
                temp_dir, true);

    }

    /**
     * Test that a graph is correctly (de)serialized.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public final void testSerialize()
            throws IOException, ClassNotFoundException {

        System.out.println("Test serialization with 1000 reqs");
        Path temp_dir = Files.createTempDirectory("tempdir");

        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests =
                processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt")));
        LinkedList<Graph<Domain>> original_user_graphs =
                    processor.computeUserGraphs(20, "test_user",
                           user_requests.values().iterator().next(), true);
        processor.saveGraphs(temp_dir, "test_user", original_user_graphs);

        File temp_file =
                new File(temp_dir.toString(), "test_user" + ".ser");
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(temp_file));
        LinkedList<Graph<Request>> deserialized_user_graphs
                = (LinkedList<Graph<Request>>) ois.readObject();

        assertEquals(original_user_graphs, deserialized_user_graphs);
    }

    /**
     * Test graph building.
     * @throws IOException
     */
    public final void testGraph() throws IOException {

        int k = 5;
        int trials = 10;

        for (int i = 0; i < trials; i++) {

            BatchProcessor processor = new BatchProcessor();
            HashMap<String, LinkedList<Request>> user_requests =
                processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/simple.txt")));

            for (Map.Entry<String, LinkedList<Request>> entry :
                    user_requests.entrySet()) {
                LinkedList<Request> requests = entry.getValue();
                Graph<Request> user_graph = 
                        processor.computeRequestGraph(requests,
                                k, new TimeSimilarity());
                    
                for (Request req : user_graph.getNodes()) {
                    NeighborList neighbors = user_graph.getNeighbors(req);
                    System.out.println(neighbors);
                    assertEquals(k, neighbors.size());
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
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests =
                processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt")));
        LinkedList<Request> requests = user_requests.get("198.36.158.8");
        Graph<Request> time_graph = processor.computeRequestGraph(
                requests, 20, new TimeSimilarity());
        // Create the domain nodes
        // (it contains every requests of a specific domain, for each domain)
        HashMap<String, Domain> time_domains =
                processor.computeDomainNodes(time_graph);

        // Test
        System.out.println("Before computation = " + time_graph.getNodes());
        System.out.println("After computation = " + time_domains.keySet());
        for (Request req : time_graph.getNodes()) {
            assertTrue(time_domains.get(req.getDomain()).contains(req));
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
        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Request>> user_requests =
                processor.computeUserLog(processor.parseFile(getClass()
                        .getResourceAsStream("/1000_http_requests.txt")));
        LinkedList<Request> requests = user_requests.get("198.36.158.8");
        Graph<Request> time_graph = processor.computeRequestGraph(
                requests, 20, new TimeSimilarity());
        // Create the domain nodes
        // (it contains every requests of a specific domain, for each domain)
        HashMap<String, Domain> time_domains =
                processor.computeDomainNodes(time_graph);
        // Compute similarity between domains and build domain graph
        Graph<Domain> time_domain_graph =
                processor.computeSimilarityDomain(time_graph, time_domains);


        // Test presence of all domains
        System.out.println("Before computation = " + time_domains.keySet());
        System.out.println("After computation = " + time_domain_graph.getNodes());
        for (Domain dom : time_domains.values()) {
            assertTrue(time_domain_graph.containsKey(dom));
        }
        
        // Test the lost of neighbors
        for (Request req : time_graph.getNodes()) {
            NeighborList nl_req = time_graph.getNeighbors(req);
            NeighborList nl_dom =
                    time_domain_graph.getNeighbors(
                            time_domains.get(req.getDomain()));
            for (Neighbor<Request> nb : nl_req) {
                if(nb.similarity != 0
                        && !nb.node.getDomain().equals(req.getDomain())) {
                    assertTrue(nl_dom.containsNode(
                            time_domains.get(nb.node.getDomain())));
                }
            } 
        }
    }
}
