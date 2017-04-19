/*
 * The MIT License
 *
 * Copyright 2016 Thomas Gilon.
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
package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 * URL Similarity.
 *
 * @author Thomas Gilon
 */
public class URLSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests URL.
     *
     * @param r1 Request 1
     * @param r2 Request 2
     * @return double : Value of the similarity
     */
    public final double similarity(final Request r1, final Request r2) {
        double counter = 0.0;
        for (int i = 0; i <= Math.min(r1.getUrl().length(),
                r2.getUrl().length()) - 1; i++) {
            if (r1.getUrl().charAt(i) == r2.getUrl().charAt(i)) {
                counter++;
            } else {
                break;
            }
        }
        return 2 * counter / (r1.getUrl().length() + r2.getUrl().length());
    }
}
