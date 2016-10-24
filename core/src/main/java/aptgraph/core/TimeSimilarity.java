package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class TimeSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests time as 1 / (1 + delta).
     * @param r1
     * @param r2
     * @return
     */
    public final double similarity(final Request r1, final Request r2) {
        return 1.0 / (1 + Math.abs(r1.getTime() - r2.getTime()));
    }
}
