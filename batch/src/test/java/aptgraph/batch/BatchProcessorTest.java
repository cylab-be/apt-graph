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
     */
    public void testAnalyze() throws IOException {
        System.out.println("Test batch server with 1000 reqs");
        BatchProcessor processor = new BatchProcessor();
        processor.analyze(
                getClass().getResourceAsStream("/1000_http_requests.txt"));

    }

}
