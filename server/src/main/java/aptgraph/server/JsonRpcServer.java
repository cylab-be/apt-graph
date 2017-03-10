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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import info.debatty.java.graphs.Graph;
import info.debatty.java.graphs.Neighbor;
import java.io.IOException;
import java.nio.file.Path;
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
public class JsonRpcServer {

    private static final Logger LOGGER
            = Logger.getLogger(JsonRpcServer.class.getName());

    private volatile org.eclipse.jetty.server.Server http_server;
    private Config config;
    private final Path input_dir;

    /**
     * Instantiate a server with default configuration.
     * @param input_dir
     */
    public JsonRpcServer(final Path input_dir) {
        config = new Config();
        this.input_dir = input_dir;
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
     * @throws java.io.IOException if the graph file cannot be read
     * @throws java.lang.ClassNotFoundException if the Graph class is not found
     * @throws java.lang.Exception if the server cannot start...
     */
    public final void start()
            throws IOException, ClassNotFoundException, Exception {

        LOGGER.log(Level.INFO, "Starting JSON-RPC server at http://{0}:{1}",
             new Object[]{config.getServerHost(), "" + config.getServerPort()});

        RequestHandler request_handler =
                new RequestHandler(input_dir);


        ObjectMapper object_mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(HistData.class, new HistDataSerializer());
        module.addSerializer(Graph.class, new GraphSerializer());
        module.addSerializer(Domain.class, new DomainSerializer());
        module.addSerializer(Neighbor.class, new NeighborSerializer());
        object_mapper.registerModule(module);

        com.googlecode.jsonrpc4j.JsonRpcServer jsonrpc_server =
                new com.googlecode.jsonrpc4j.JsonRpcServer(
                        object_mapper,
                        request_handler);


        QueuedThreadPool thread_pool = new QueuedThreadPool(
                config.getMaxThreads(),
                config.getMinThreads(),
                config.getIdleTimeout(),
                new ArrayBlockingQueue<Runnable>(
                        config.getMaxPendingRequests()));

        http_server = new org.eclipse.jetty.server.Server(thread_pool);
        //http_server = new org.eclipse.jetty.server.Server();

        ServerConnector http_connector = new ServerConnector(http_server);
        http_connector.setHost(config.getServerHost());
        http_connector.setPort(config.getServerPort());

        http_server.setConnectors(new Connector[]{http_connector});
        http_server.setHandler(new JettyHandler(jsonrpc_server));

        http_server.start();
    }

    /**
     * Start the server in a separate thread and return immediately.
     */
    public final void startInBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    start();

                } catch (ClassNotFoundException ex) {
                    LOGGER.log(Level.SEVERE, "Class not found", ex);

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Exception", ex);
                }
            }
        }).start();
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
