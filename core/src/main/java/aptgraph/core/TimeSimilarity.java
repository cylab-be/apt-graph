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
package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 * Time Similarity.
 *
 * @author Thibault Debatty
 */
public class TimeSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests time as 1 / (1 + |delta|).
     *
     * @param r1 Request 1
     * @param r2 Request 2
     * @return double : Value of the similarity
     */
    public final double similarity(final Request r1, final Request r2) {
        return 1.0 / (1 + Math.abs(
                Math.round(r1.getTime() / 1000.0)
                - Math.round(r2.getTime() / 1000.0)));
    }
}
