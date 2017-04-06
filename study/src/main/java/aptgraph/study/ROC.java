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
package aptgraph.study;

import aptgraph.core.Domain;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Gilon
 */
public final class ROC {
    private ROC() {
    }

    private static final Logger LOGGER
            = Logger.getLogger(ROC.class.getName());

    /**
     * Build the file with ROC curve based on the ranking.
     * @param ranking
     * @param n_dom_tot
     * @param n_apt_tot
     * @param output_file
     */
    public static void makeROC(
            final TreeMap<Double, LinkedList<Domain>> ranking,
            final int n_dom_tot, final int n_apt_tot,
            final String output_file) {
        ArrayList<double[]> roc_curve =
                computeROC(ranking, n_dom_tot, n_apt_tot);
        LOGGER.log(Level.INFO, "ROC curve created");
        try {
            exportROC(roc_curve, new FileOutputStream(output_file));
        } catch (IOException ex) {
            Logger.getLogger(ROC.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOGGER.log(Level.INFO, "ROC curve exported to {0}",
                output_file);
    }

    /**
     * Compute the ROC curve based on the ranking.
     * @param ranking
     * @param n_dom_tot
     * @param n_apt_tot
     * @return ROC_curve
     */
    private static ArrayList<double[]> computeROC(
            final TreeMap<Double, LinkedList<Domain>> ranking,
            final int n_dom_tot, final int n_apt_tot) {
        double n_dom = 0;
        double n_apt = 0;
        ArrayList<double[]> roc_curve = new ArrayList<double[]>();
        for (Entry<Double, LinkedList<Domain>> entry : ranking.entrySet()) {
            LinkedList<Domain> list = entry.getValue();
            for (Domain dom : list) {
                if (dom.getName().endsWith(".apt")) {
                    n_apt += 1.0;
                } else {
                    n_dom += 1.0;
                }
                roc_curve.add(new double[]
                    {n_dom / n_dom_tot, n_apt / n_apt_tot});
            }
        }
        return roc_curve;
    }

    /**
     * Export the ROC curve in CSV.
     * @param ROC_curve
     * @param output_file
     * @throws IOException
     */
    private static void exportROC(
            final ArrayList<double[]> roc_curve,
            final OutputStream output_file)
            throws IOException {
        for (double[] pfa_pd : roc_curve) {
            output_file.write((pfa_pd[0] + "," + pfa_pd[1] + "\n")
                    .getBytes("UTF-8"));
        }
    }
}
