package aptgraph.core;

import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class DomainSimilarity
        implements SimilarityInterface<Request>, Serializable {

    /**
     * Compute the similarity between requests Domains.
     * @param r1
     * @param r2
     * @return
     */
    public final double similarity(final Request r1, final Request r2) {
        double counter = 0.0;
        String[] domain_r1 = r1.getDomain().split("[.]");
        String[] domain_r2 = r2.getDomain().split("[.]");
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
    }
}
