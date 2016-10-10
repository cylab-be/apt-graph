/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package aptgraph.server;

import aptgraph.core.Request;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import info.debatty.java.graphs.Graph;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author Thibault Debatty
 */
public class Server {

    private static final Logger LOGGER
            = Logger.getLogger(Server.class.getName());

    private volatile org.eclipse.jetty.server.Server http_server;
    private Config config;
    private final InputStream input_file;

    /**
     * Instantiate a server with default configuration.
     *
     * @param input_file
     */
    public Server(final InputStream input_file) {
        config = new Config();
        this.input_file = input_file;
    }

    /**
     * Set a non-default config.
     * @param config
     */
    public final void setConfig(final Config config) {
        this.config = config;
    }

    /**
     * Start the server, blocking. This method will only return if the server
     * crashed...
     */
    public final void start() {

        LOGGER.info("Reading graphs from disk...");
        Graph<Request> graph = null;
        try {
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_file));
            graph = (Graph<Request>) input.readObject();
            input.close();

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not read input file", ex);
            return;

        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Class not found!", ex);
            return;
        }

        LOGGER.info("Graph has " + graph.size() + " nodes");

        LOGGER.info("Starting JSON-RPC server at http://" + config.server_host
                + ":" + config.server_port);
        RequestHandler request_handler = new RequestHandler(graph);
        JsonRpcServer jsonrpc_server = new JsonRpcServer(request_handler);

        QueuedThreadPool thread_pool = new QueuedThreadPool(
                config.max_threads,
                config.min_threads,
                config.idle_timeout,
                new ArrayBlockingQueue<Runnable>(config.max_pending_requests));

        http_server = new org.eclipse.jetty.server.Server(thread_pool);
        //http_server = new org.eclipse.jetty.server.Server();

        ServerConnector http_connector = new ServerConnector(http_server);
        http_connector.setHost(config.server_host);
        http_connector.setPort(config.server_port);

        http_server.setConnectors(new Connector[]{http_connector});
        http_server.setHandler(new JettyHandler(jsonrpc_server));

        try {
            http_server.start();
        } catch (Exception ex) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to start server: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     *
     */
    public final void stop() {
        LOGGER.info("Stopping server...");
        try {
            http_server.stop();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to stop server properly :(", ex);
        }
    }
}
