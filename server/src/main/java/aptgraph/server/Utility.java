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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class definition which defines some tools for the Server.
 *
 * @author Thomas Gilon
 */
public final class Utility {

    private Utility() {
    }

    /**
     * Compute the mean of an ArrayList&lt;Double&gt;.
     *
     * @param list List
     * @return double : Mean of the given list
     */
    public static double getMean(final ArrayList<Double> list) {
        double sum = 0.0;
        for (double i : list) {
            sum += i;
        }
        return sum / list.size();
    }

    /**
     * Compute the mean and variance of an ArrayList&lt;Double&gt;.
     *
     * @param list List
     * @return double[] : [mean variance]
     */
    public static double[] getMeanVariance(
            final ArrayList<Double> list) {
        double mean = getMean(list);
        double sum = 0.0;
        for (double i : list) {
            sum += (i - mean) * (i - mean);
        }
        return new double[]{mean, sum / list.size()};
    }

    /**
     * Compute the z score of a value.
     *
     * @param mean Mean
     * @param variance Variance
     * @param value Value
     * @return double : Z score of the given value
     */
    public static double getZ(final double mean, final double variance,
            final Double value) {
        return (value - mean) / Math.sqrt(variance);
    }

    /**
     * Compute the absolute value from the z score.
     *
     * @param mean Mean
     * @param variance Variance
     * @param z Z score
     * @return double : Absolute value of the given z score
     */
    public static double fromZ(final double mean, final double variance,
            final Double z) {
        return mean + z * Math.sqrt(variance);
    }

    /**
     * Compute maximum and minimum of an ArrayList&lt;Double&gt;.
     *
     * @param list List
     * @return ArrayList&lt;Double&gt; : [maximum minimum]
     */
    public static ArrayList<Double> getMaxMin(final ArrayList<Double> list) {
        Double max = 0.0;
        Double min = Double.MAX_VALUE;
        for (Double d : list) {
            max = Math.max(d, max);
            min = Math.min(d, min);
        }
        ArrayList<Double> max_min = new ArrayList<Double>(2);
        max_min.add(max);
        max_min.add(min);
        return max_min;
    }

    /**
     * Compute the absolute prune threshold based on z score.
     *
     * @param mean Mean
     * @param variance Variance
     * @param z_prune_threshold Z score of prune threshold
     * @return double : Prune threshold in absolute value
     */
    public static double computePruneThreshold(final double mean,
            final double variance,
            final Double z_prune_threshold) {
        double prune_threshold
                = Utility.fromZ(mean, variance, z_prune_threshold);
        if (prune_threshold < 0) {
            prune_threshold = 0;
        }
        return prune_threshold;
    }

    /**
     * Compute the absolute maximum cluster size based on z score.
     *
     * @param mean Mean
     * @param variance Variance
     * @param z_max_cluster_size Z score of max cluster size
     * @return double : Max cluster size in absolute value
     */
    public static double computeClusterSize(final double mean,
            final double variance,
            final Double z_max_cluster_size) {
        double max_cluster_size_temp = Utility.fromZ(mean, variance,
                z_max_cluster_size);
        int max_cluster_size = (int) Math.round(max_cluster_size_temp);
        if (max_cluster_size < 0) {
            max_cluster_size = 0;
        }
        return max_cluster_size;
    }

    /**
     * Remove a list of nodes from a given graph (and update graph).
     *
     * @param <U> Class of the list of nodes
     * @param graph Graph to clean
     * @param nodes List of nodes
     */
    static <U> void remove(final Graph<U> graph, final LinkedList<U> nodes) {
        HashMap<U, NeighborList> map = graph.getHashMap();

        // Delete the node
        for (U node : nodes) {
            map.remove(node);
        }
        // Delete the invalid edges to avoid "NullPointerException"
        Iterator<Map.Entry<U, NeighborList>> iterator_1
                = map.entrySet().iterator();
        while (iterator_1.hasNext()) {
            Map.Entry<U, NeighborList> entry = iterator_1.next();
            NeighborList neighborlist = entry.getValue();

            // Delete reference to deleted node
            // => for() can't be used
            // (see http://stackoverflow.com/questions/223918/)
            Iterator<Neighbor> iterator_2 = neighborlist.iterator();
            while (iterator_2.hasNext()) {
                Neighbor<U> neighbor = iterator_2.next();
                if (nodes.contains(neighbor.getNode())) {
                    iterator_2.remove();
                }
            }
        }
    }

    /**
     * Compute the histogram of a given list.
     *
     * @param list List
     * @param min Minimum
     * @param max Maximum
     * @param step Step between bins in the histogram
     * @return HistData : Histogram data based on the list
     */
    public static HistData computeHistogram(
            final ArrayList<Double> list,
            final double min,
            final double max,
            final double step) {
        HistData hist_data = new HistData();
        for (Double i = min; i <= max + step; i += step) {
            hist_data.put(i, 0.0);
        }
        for (Double d1 : list) {
            Double diff = Double.MAX_VALUE;
            Double bin = hist_data.keySet().iterator().next();
            for (Double d2 : hist_data.keySet()) {
                if (Math.abs(d2 - d1) < diff) {
                    diff = Math.abs(d2 - d1);
                    bin = d2;
                }
            }
            hist_data.put(bin, hist_data.get(bin)
                    + 1.0); // / list.size() * 100);
        }
        return hist_data;
    }

    /**
     * Clean the histogram to avoid consecutive identical values. This is needed
     * to speed up the web site plot.
     *
     * @param hist_data_in Histogram to clean up
     * @return HistData Histogram cleaned
     */
    public static HistData cleanHistogram(final HistData hist_data_in) {
        HistData hist_data_out = new HistData();
        LinkedList<Double> keys = hist_data_in.getKeys();
        LinkedList<Double> values = hist_data_in.getValues();
        hist_data_out.put(keys.get(0), values.get(0));
        for (int i = 1; i < keys.size() - 1; i++) {
            if ((Math.abs(values.get(i) - values.get(i - 1))) > 1E-10
                    || (Math.abs(values.get(i) - values.get(i + 1))) > 1E-10) {
                hist_data_out.put(keys.get(i), values.get(i));
            }
        }
        hist_data_out.put(keys.get(keys.size() - 1),
                values.get(keys.size() - 1));
        return hist_data_out;
    }

    /**
     * Sorting function, based on the given index.
     *
     * @param list_domain List of domains
     * @param index Map of the indexes associated to each domain
     * @return ArrayList&lt;Domain&gt; : Sorted list of domains
     */
    public static ArrayList<Domain> sortByIndex(final List<Domain> list_domain,
            final HashMap<Domain, Double> index) {
        Domain selected;
        ArrayList<Domain> sorted = new ArrayList<Domain>();
        while (!list_domain.isEmpty()) {
            selected = list_domain.get(0);
            for (Domain dom : list_domain) {
                if (index.get(dom) < index.get(selected)) {
                    selected = dom;
                }
            }
            sorted.add(selected);
            list_domain.remove(selected);
        }

        return sorted;
    }
}
