package aptgraph.batch;

import aptgraph.core.Request;
import info.debatty.java.graphs.Graph;
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
                temp_dir);

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
        LinkedList<Graph<Request>> original_user_graphs =
                    processor.computeUserGraphs(20, "test_user",
                            user_requests.values().iterator().next());
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
                String user = entry.getKey();
                LinkedList<Request> requests = entry.getValue();
                LinkedList<Graph<Request>> user_graphs =
                    processor.computeUserGraphs(k, user, requests);
                for (Graph<Request> graph : user_graphs) {
                    for (Request req : graph.getNodes()) {
                        NeighborList neighbors = graph.getNeighbors(req);
                        System.out.println(neighbors);
                        assertEquals(k, neighbors.size());
                    }
                }
            }
        }
    }
}
