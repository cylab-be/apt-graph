package aptgraph.server;

/**
 *
 * @author Thibault Debatty
 */
public final class Main {

    private Main() {
    }

    /**
     *
     * @param args
     */
    public static void main(final String[] args) {
        Server server = new Server();
        server.start();
    }
}