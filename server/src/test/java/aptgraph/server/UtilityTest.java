/*
 * The MIT License
 *
 * Copyright 2017 Thomas Gilon.
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

import aptgraph.core.Domain;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import info.debatty.java.graphs.NeighborList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import static junit.framework.Assert.assertFalse;

/**
 *
 * @author Thomas Gilon
 */
public class UtilityTest {

    /**
     * Test the effectiveness of the suppression of a node in a graph.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void testRemove()
            throws IOException, ClassNotFoundException {
        System.out.println("\nTest : remove");
        
        // Creation of the data
        Path input_dir = Paths.get("src/test/resources/dummyDir");
        RequestHandler handler = new RequestHandler(input_dir);
        handler.getUsers();
        handler.getMemory().setUsersList(handler.getMemory().getAllUsersList());
        handler.loadUsersGraphs(System.currentTimeMillis());
        handler.getMemory().setFeatureWeights(new double[]{0.7, 0.1, 0.2});
        handler.getMemory().setFeatureOrderedWeights(new double[]{0.0, 0.0});
        LinkedList<Graph<Domain>> merged_graph_users
                    = handler.computeUsersGraph();
        double[] users_weights = new double[merged_graph_users.size()];
        for (int i = 0; i < merged_graph_users.size(); i++) {
            users_weights[i] = 1.0 / merged_graph_users.size();
        }
        Graph<Domain> merged_graph
                = handler.computeFusionGraphs(merged_graph_users, "",
                        users_weights, new double[] {0.0}, "all");
        
        // Test 
        Domain first_node = merged_graph.first();
        LinkedList<Domain> nodes = new LinkedList<Domain>();
        nodes.add(first_node);
        Utility.remove(merged_graph, nodes);
        assertFalse(merged_graph.containsKey(first_node));
        for (Domain dom : merged_graph.getNodes()) {
            NeighborList nl = merged_graph.getNeighbors(dom);
            for (Neighbor<Domain> nb : nl) {
                assertFalse(nb.node.deepEquals(first_node));
            }
        }
    }
    
}
