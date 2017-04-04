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
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 * @author Thomas Gilon
 */
public class Output implements Serializable {
    private String name = "";
    private LinkedList<Graph<Domain>> filtered_white_listed;
    private String stdout;
    private HistData hist_pruning;
    private HistData hist_cluster;

    /**
     * Return name.
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * Return filtered_white_listed.
     * @return
     */
    public final LinkedList<Graph<Domain>> getFilteredWhiteListed() {
        return filtered_white_listed;
    }

    /**
     * Return standard output.
     * @return
     */
    public final String getStdout() {
        return stdout;
    }

    /**
     * Return histogram for pruning.
     * @return
     */
    public final HistData getHistPruning() {
        return hist_pruning;
    }

    /**
     * Return histogram of cluster size.
     * @return
     */
    public final HistData getHistCluster() {
        return hist_cluster;
    }

    /**
     * Set output name.
     * @param name
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * Set filtered_white_listed.
     * @param filtered_white_listed
     */
    public final void setFilteredWhiteListed(
            final LinkedList<Graph<Domain>> filtered_white_listed) {
        this.filtered_white_listed = filtered_white_listed;
    }

    /**
     * Set stdout.
     * @param stdout
     */
    public final void setStdout(final String stdout) {
        this.stdout = stdout;
    }

    /**
     * Set histogram for pruning.
     * @param hist_pruning
     */
    public final void setHistPruning(
            final HistData hist_pruning) {
        this.hist_pruning = hist_pruning;
    }

    /**
     * Set histogram for cluster size.
     * @param hist_cluster
     */
    public final void setHistCluster(
            final HistData hist_cluster) {
        this.hist_cluster = hist_cluster;
    }

    /**
     * Return ReturnAnalyze name.
     * @return
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Compare two outputs.
     * @param obj
     * @return boolean
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Output output = (Output) obj;
        if (this.filtered_white_listed.equals(output.filtered_white_listed)
            && this.hist_cluster.equals(output.hist_cluster)
            && this.hist_pruning.equals(output.hist_pruning)
            && this.name.equals(output.name)
            && this.stdout.equals(output.stdout)) {
            return true;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 23 * hash + this.name.hashCode()
                + this.filtered_white_listed.hashCode()
                + this.hist_cluster.hashCode()
                + this.hist_pruning.hashCode()
                + this.stdout.hashCode();
        return hash;
    }
}

