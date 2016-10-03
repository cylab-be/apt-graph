package aptgraph.batch;

/**
 *
 * @author Thibault Debatty
 */
class Request {
    int time;
    String client;
    String url;

    @Override
    public String toString() {
        return time + "\t" + url + " " + client;
    }

}
