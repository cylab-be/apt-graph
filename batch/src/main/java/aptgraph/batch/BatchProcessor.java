package aptgraph.batch;

import aptgraph.core.Request;
import aptgraph.core.TimeSimilarity;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Node;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Thibault Debatty
 */
public class BatchProcessor {

    private static final Logger LOGGER = Logger.getLogger(
            BatchProcessor.class.getName());
    private static final String REGEX =
            "^(\\d{10})\\..*\\s(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s"
            + "(\\S+)\\s(\\S+)\\s(\\S+)\\s(\\S+)\\s.*$";

    private final Pattern pattern;


    /**
     *
     */
    public BatchProcessor() {
        pattern = Pattern.compile(REGEX);
    }

    /**
     *
     * @param input_file
     * @param output_file
     * @throws IOException if we cannot read the input file
     */
    public final void analyze(
            final InputStream input_file, final FileOutputStream output_file)
            throws IOException {

        LOGGER.info("Read and parse input file...");
        LinkedList<Request> requests = parseFile(input_file);

        LOGGER.info("Found " + requests.size() + " requests...");
        LinkedList<Node<Request>> nodes = new LinkedList<Node<Request>>();
        int i = 0;
        for (Request r : requests) {
            nodes.add(new Node<Request>(String.valueOf(i), r));
            i++;
        }

        LOGGER.info("Build the time based graph...");
        ThreadedNNDescent<Request> nndes = new ThreadedNNDescent<Request>();
        nndes.setSimilarity(new TimeSimilarity());
        Graph<Request> time_graph = nndes.computeGraph(nodes);
        System.out.println(time_graph.get(nodes.getFirst()));

        //LOGGER.info("Build URL graph...");

        LOGGER.info("Save graphs to disk...");
        ObjectOutputStream output = new ObjectOutputStream(
                new BufferedOutputStream(output_file));
        output.writeObject(time_graph);
        output.close();
    }


    /**
     * Read and parse the input file line by line.
     * @param file
     * @return
     */
    private LinkedList<Request> parseFile(final InputStream file)
            throws IOException {

        LinkedList<Request> requests = new LinkedList<Request>();
        BufferedReader in = new BufferedReader(new InputStreamReader(file));
        String line = null;

        while ((line = in.readLine()) != null) {
            requests.add(parseLine(line));
        }

        return requests;
    }

    private Request parseLine(final String line) {

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
