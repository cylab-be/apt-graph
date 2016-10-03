package aptgraph.batch;

import java.io.IOException;
import junit.framework.TestCase;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessorTest extends TestCase {

    /**
     * Test of analyze method, of class BatchProcessor.
     * @throws java.io.IOException if the input file is not found
     */
    public final void testAnalyze() throws IOException {
        System.out.println("Test batch server with 1000 reqs");
        BatchProcessor processor = new BatchProcessor();
        processor.analyze(
                getClass().getResourceAsStream("/1000_http_requests.txt"));

    }

}
