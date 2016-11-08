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
        if (r1.getUrl().equals(r2.getUrl()))  {
            return 0;
        } else {
            return 1;
        }
    }
}
