package aptgraph.core;

import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class Request implements Serializable {
    public int time;
    public String client;
    public String url;

    @Override
    public String toString() {
        return time + "\t" + url + " " + client;
    }

}
