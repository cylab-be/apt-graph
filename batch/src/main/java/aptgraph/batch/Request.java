package aptgraph.batch;

import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
class Request implements Serializable {
    int time;
    String client;
    String url;

    @Override
    public String toString() {
        return time + "\t" + url + " " + client;
    }

}
