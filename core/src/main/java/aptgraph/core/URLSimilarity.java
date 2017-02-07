package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class URLSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests URL.
     * @param r1
     * @param r2
     * @return
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
