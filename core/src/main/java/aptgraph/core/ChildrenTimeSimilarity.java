package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author Thibault Debatty
 */
public class ChildrenTimeSimilarity
        implements SimilarityInterface<Request>, Serializable {


//    private static final Logger LOGGER = Logger.getLogger(
//            ChildrenTimeSimilarity.class.getName());


    /**
     * Compute the similarity between requests time as 1 / (1 + delta).
     * @param r1
     * @param r2
     * @return
     */
    public final double similarity(final Request r1, final Request r2) {
//        LOGGER.log(Level.INFO, "Children Similarity = r1, {0} + r2, {1}",
//                new Object[]{r1.getDomain(), r2.getDomain()});
        if (r1.getTime() > r2.getTime()) {
            return 1.0 / (1 + (r1.getTime() - r2.getTime()));
        } else {
            return 0.0;
        }
    }
}
