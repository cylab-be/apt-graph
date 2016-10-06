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

import aptgraph.core.Request;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Node;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thibault Debatty
 */
public class RequestHandler {
    private final Graph<Request> graph;

    RequestHandler(final Graph<Request> graph) {
        this.graph = graph;
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
    public final List<Graph<Request>> dummy() {
        //Node<Request> node = graph.getNodes().iterator().next();
        //System.out.println(node);
        //System.out.println(graph.get(node));

        // Fusion

        // Prune
        graph.prune(0.9);

        // Cluster
        ArrayList<Graph<Request>> clusters = graph.connectedComponents();

        // Filter
        LinkedList<Graph<Request>> filtered = new LinkedList<Graph<Request>>();
        for (Graph<Request> subgraph : clusters) {
            if (subgraph.size() < 10) {
                filtered.add(subgraph);
            }
        }
        System.out.println("Found " + filtered.size() + " clusters");
        return filtered;

    }

    private List<Node<String>> readFile(final InputStream input)
            throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input));
        ArrayList<Node<String>> nodes = new ArrayList<Node<String>>();
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            nodes.add(new Node(String.valueOf(i), line));
            i++;
        }
        reader.close();
        return nodes;
    }
}