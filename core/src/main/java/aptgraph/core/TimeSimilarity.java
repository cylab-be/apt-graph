package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class TimeSimilarity implements SimilarityInterface<Request>, Serializable {

    public double similarity(final Request r1, final Request r2) {
        return 1.0 / (1 + Math.abs(r1.time - r2.time));
    }
}