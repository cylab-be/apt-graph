/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty & Thomas Gilon.
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
 * Domain Similarity.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
 */
public class DomainSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests Domains.
     *
     * @param r1 Request 1
     * @param r2 Request 2
     * @return double : Value of the similarity
     */
    public final double similarity(final Request r1, final Request r2) {
        double counter = 0.0;
        String[] domain_r1 = r1.getDomain().split("[.]");
        String[] domain_r2 = r2.getDomain().split("[.]");
        if (domain_r1[domain_r1.length - 1]
                .equals(domain_r2[domain_r2.length - 1])
                && domain_r1.length > 1 && domain_r2.length > 1) {
            for (int i = 1; i <= Math.min(domain_r1.length,
                    domain_r2.length) - 1; i++) {
                if (domain_r1[domain_r1.length - i - 1]
                        .equals(domain_r2[domain_r2.length - i - 1])) {
                    counter++;
                } else {
                    break;
                }
            }
            return counter / (Math.max(domain_r1.length, domain_r2.length) - 1);
        } else {
            if (r1.getDomain().equals(r2.getDomain())) {
                counter++;
            }
            return counter / (Math.max(domain_r1.length, domain_r2.length));
        }
    }
}
