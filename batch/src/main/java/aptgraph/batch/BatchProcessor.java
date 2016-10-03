package aptgraph.batch;

import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Node;
import info.debatty.java.graphs.SimilarityInterface;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessor {

    private final String regex =
            "^(\\d{10})\\..*\\s(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s"
            + "(\\S+)\\s(\\S+)\\s(\\S+)\\s(\\S+)\\s.*$";
    private final Pattern pattern;

    /**
     *
     */
    public BatchProcessor() {
        pattern = Pattern.compile(regex);
    }

    /**
     *
     * @param file
     * @throws IOException if we cannot read the input file
     */
    public final void analyze(final InputStream file) throws IOException {

        System.out.println("Read and parse file...");
        LinkedList<Request> requests = parse(file);

        System.out.println("Convert to nodes...");
        LinkedList<Node<Request>> nodes = new LinkedList<Node<Request>>();
        int i = 0;
        for (Request r : requests) {
            nodes.add(new Node<Request>(String.valueOf(i), r));
            i++;
        }

        System.out.println("Build the time based graph...");
        ThreadedNNDescent<Request> nndes = new ThreadedNNDescent<Request>();
        nndes.setSimilarity(new SimilarityInterface<Request>() {

            public double similarity(final Request r1, final Request r2) {
                return 1.0 / (1 + Math.abs(r1.time - r2.time));
            }
        });
        Graph<Request> time_graph = nndes.computeGraph(nodes);
        System.out.println(time_graph.get(nodes.getFirst()));

        System.out.println("Build URL graph...");

        System.out.println("Save graphs to disk...");

    }

    /**
     * Read and parse the input file line by line.
     * @param file
     * @return
     */
    private LinkedList<Request> parse(final InputStream file)
            throws IOException {

        LinkedList<Request> requests = new LinkedList<Request>();
        BufferedReader in = new BufferedReader(new InputStreamReader(file));
        String line = null;

        while ((line = in.readLine()) != null) {
            requests.add(parse(line));
        }

        return requests;
    }

    private Request parse(final String line) {

        Matcher match = pattern.matcher(line);

        if (!match.matches()) {
            throw new IllegalArgumentException("Regex did not match " + line);
        }

        Request request = new Request();
        request.time = Integer.valueOf(match.group(1));
        request.client = match.group(2);
        request.url = match.group(6);
        return request;
    }

}
