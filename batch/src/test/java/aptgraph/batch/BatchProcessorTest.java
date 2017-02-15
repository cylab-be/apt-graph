package aptgraph.batch;

import aptgraph.core.Request;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.NeighborList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import junit.framework.TestCase;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessorTest extends TestCase {

    /**
     * Test of analyze method, of class BatchProcessor.
     *
     * @throws java.io.IOException if the input file is not found
     */
    public final void testAnalyze() throws IOException {
        System.out.println("Test batch server with 1000 reqs");
        File temp_file = File.createTempFile("tempfile", ".tmp");

        BatchProcessor processor = new BatchProcessor();
        processor.analyze(10,
                getClass().getResourceAsStream("/1000_http_requests.txt"),
                new FileOutputStream(temp_file));

    }

    public final void testSerialize() throws IOException, ClassNotFoundException {
        System.out.println("Test serialization with 1000 reqs");
        File temp_file = File.createTempFile("tempfile", ".tmp");

        BatchProcessor processor = new BatchProcessor();
        HashMap<String, LinkedList<Graph<Request>>> original_user_graphs =
                processor.computeGraphs(10,
                getClass().getResourceAsStream("/1000_http_requests.txt"));
        processor.saveGraphs(original_user_graphs,
                new FileOutputStream(temp_file));

        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(temp_file));
        HashMap<String, LinkedList<Graph<Request>>> deserialized_user_graphs
                = (HashMap<String, LinkedList<Graph<Request>>>) ois.readObject();

        assertEquals(original_user_graphs, deserialized_user_graphs);
    }
}
