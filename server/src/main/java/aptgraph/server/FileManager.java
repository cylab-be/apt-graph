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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Gilon
 */
public final class FileManager {
    private FileManager() {
    }

    private static final Logger LOGGER
            = Logger.getLogger(FileManager.class.getName());

    /**
     * Load the list of graphs for a given user.
     * @param input_dir
     * @param user
     * @return List of graphs
     */
    public static LinkedList<Graph<Domain>> getUserGraphs(
            final Path input_dir, final String user) {
        LOGGER.log(Level.INFO, "Reading graphs of user {0} from disk...", user);
        LinkedList<Graph<Domain>> user_graphs =
                new LinkedList<Graph<Domain>>();
        try {
            File file = new File(input_dir.toString(), user + ".ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            user_graphs = (LinkedList<Graph<Domain>>) input.readObject();
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        return user_graphs;
    }

    /**
     * Load the value of k used for k-NN Graphs.
     * @param input_dir
     * @return List of graphs
     */
    public static int getK(final Path input_dir) {
        LOGGER.log(Level.INFO, "Reading k value from disk...");
        int k = 0;
        try {
            File file = new File(input_dir.toString(), "k.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            k = (int) input.readInt();
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        }

        return k;
    }
}
