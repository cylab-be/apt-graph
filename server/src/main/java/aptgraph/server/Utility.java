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

import java.util.ArrayList;

/**
 *
 * @author Thomas Gilon
 */
public final class Utility {
    private Utility() {
    }

    /**
     * Compute the mean of an ArrayList<Double>.
     * @param list
     * @return mean
     */
    public static double getMean(final ArrayList<Double> list) {
            double sum = 0.0;
            for (double i : list) {
                sum += i;
            }
            return sum / list.size();
    }

    /**
     * Compute the mean and variance of an ArrayList<Double>.
     * @param list
     * @return ArrayList<Double> mean_variance
     */
    public static ArrayList<Double> getMeanVariance(
            final ArrayList<Double> list) {
        double mean = getMean(list);
        double sum = 0.0;
        for (double i :list) {
            sum += (i - mean) * (i - mean);
        }
        ArrayList<Double> out = new ArrayList<Double>(2);
        out.add(mean);
        out.add(sum / list.size());
        return out;
    }

    /**
     * Compute the z score of a value.
     * @param mean
     * @param variance
     * @param value
     * @return z
     */
    public static double getZ(final double mean, final double variance,
            final Double value) {
        return (value - mean) / Math.sqrt(variance);
    }

    /**
     * Compute the absolute value from the z score.
     * @param mean
     * @param variance
     * @param z
     * @return absolute value
     */
    public static double fromZ(final double mean, final double variance,
            final Double z) {
        return mean + z * Math.sqrt(variance);
    }

    /**
     * Compute maximum and minimum of an ArrayList<Double>.
     * @param list
     * @return ArrayList<Double> max_min
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
}
