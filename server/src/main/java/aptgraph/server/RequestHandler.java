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

import com.mongodb.client.MongoDatabase;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Node;
import info.debatty.java.graphs.SimilarityInterface;
import info.debatty.java.graphs.build.GraphBuilder;
import info.debatty.java.graphs.build.ThreadedNNDescent;
import info.debatty.java.stringsimilarity.JaroWinkler;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final MongoDatabase db;

    /**
     *
     */
    public RequestHandler() {
        this.db = null;
    }

    /**
     * A test json-rpc call, with no argument, that should return "hello".
     * @return
     */
    public final String test() {
        return "hello";
    }

    /**
     * A dummy method that returns some clusters of nodes and edges.
     * @return
     */
    public final List<Graph<String>> dummy() {

        // Read some dummy strings
        List<Node<String>> nodes = GraphBuilder.readFile(
                getClass().getClassLoader().getResource("726-unique-spams").getFile());

        // Compute k-nn graph
        ThreadedNNDescent<String> nndes = new ThreadedNNDescent<String>();
        nndes.setSimilarity(new SimilarityInterface<String>() {

            public double similarity(String value1, String value2) {
                JaroWinkler jw = new JaroWinkler();
                return jw.similarity(value1, value2);
            }
        });
        Graph<String> graph = nndes.computeGraph(nodes);

        // Prune
        graph.prune(0.7);

        // Cluster
        ArrayList<Graph<String>> clusters = graph.connectedComponents();

        // Filter
        LinkedList<Graph<String>> filtered = new LinkedList<Graph<String>>();
        for (Graph<String> subgraph : clusters) {
            if (subgraph.size() < 3) {
                filtered.add(subgraph);
            }
        }

        return filtered;

    }
}