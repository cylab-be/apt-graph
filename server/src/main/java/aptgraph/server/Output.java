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
import java.util.TreeMap;

/**
 * Output object definition. This contains all the values returned from the
 * server and needed on the web page.
 *
 * @author Thomas Gilon
 */
public class Output implements Serializable {

    private LinkedList<Graph<Domain>> filtered_white_listed
            = new LinkedList<Graph<Domain>>();
    private String stdout = "";
    private TreeMap<Double, LinkedList<String>> ranking = new TreeMap<Double,
            LinkedList<String>>();
    private HistData hist_similarities = new HistData();
    private HistData hist_cluster = new HistData();

    /**
     * Return the list of cluster after filtering and white listing.
     *
     * @return LinkedList&lt;Graph&lt;Domain&gt;&gt; : List of cluster after
     * filtering and white listing
     */
    public final LinkedList<Graph<Domain>> getFilteredWhiteListed() {
        return filtered_white_listed;
    }

    /**
     * Return Standard output on UI.
     *
     * @return String : Standard output on UI
     */
    public final String getStdout() {
        return stdout;
    }

    /**
     * Return info of Ranking.
     *
     * @return TreeMap&lt;Double, LinkedList&lt;String&gt;&gt; : Info of Ranking
     */
    public final TreeMap<Double, LinkedList<String>> getRanking() {
        return ranking;
    }

    /**
     * Return histogram data of similarities of merged graph.
     *
     * @return HistData : Histogram data of similarities of merged graph
     */
    public final HistData getHistDataSimilarities() {
        return hist_similarities;
    }

    /**
     * Return histogram data of cluster sizes.
     *
     * @return HistData : Histogram data of cluster sizes
     */
    public final HistData getHistDataClusters() {
        return hist_cluster;
    }

    /**
     * Set list of cluster after filtering and white listing.
     *
     * @param filtered_white_listed List of cluster after filtering and white
     * listing
     */
    public final void setFilteredWhiteListed(
            final LinkedList<Graph<Domain>> filtered_white_listed) {
        this.filtered_white_listed = filtered_white_listed;
    }

    /**
     * Set standard output on UI.
     *
     * @param stdout Standard output on UI
     */
    public final void setStdout(final String stdout) {
        this.stdout = stdout;
    }

    /**
     * Set info of Ranking.
     *
     * @param ranking Info of Ranking
     */
    public final void setRanking(
            final TreeMap<Double, LinkedList<String>> ranking) {
        this.ranking = ranking;
    }

    /**
     * Set histogram data of similarities of merged graph.
     *
     * @param hist_similarities Histogram data of similarities of merged graph
     */
    public final void setHistDataSimilarities(
            final HistData hist_similarities) {
        this.hist_similarities = hist_similarities;
    }

    /**
     * Set histogram data of cluster sizes.
     *
     * @param hist_cluster Histogram data of cluster sizes
     */
    public final void setHistDataClusters(
            final HistData hist_cluster) {
        this.hist_cluster = hist_cluster;
    }

    /**
     * Compare two outputs.
     *
     * @param obj Output to compare
     * @return boolean : True if two outputs are equal
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
                && this.hist_similarities.equals(output.hist_similarities)
                && this.stdout.equals(output.stdout)
                && this.ranking.equals(output.ranking)) {
            return true;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 23 * hash
                + this.filtered_white_listed.hashCode()
                + this.hist_cluster.hashCode()
                + this.hist_similarities.hashCode()
                + this.stdout.hashCode()
                + this.ranking.hashCode();
        return hash;
    }
}
