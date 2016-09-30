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

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.ArrayBlockingQueue;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author Thibault Debatty
 */
public class Server {
    private org.eclipse.jetty.server.Server http_server;
    private Config config;

    /**
     * Instantiate a server with default configuration.
     */
    public Server() {
        config = new Config();
    }

    /**
     * Start the server, blocking.
     * This method will only return if the server crashed...
     */
    public final void start() {
        // Connect to mongodb
        MongoClient mongodb = new MongoClient(
                config.mongo_host, config.mongo_port);
        MongoDatabase mongodb_database = mongodb.getDatabase(config.mongo_db);

        // Create and run HTTP / JSON-RPC server
        RequestHandler datastore_handler = new RequestHandler(mongodb_database);
        JsonRpcServer jsonrpc_server = new JsonRpcServer(datastore_handler);

        QueuedThreadPool thread_pool = new QueuedThreadPool(
                config.max_threads,
                config.min_threads,
                config.idle_timeout,
                new ArrayBlockingQueue<Runnable>(config.max_pending_requests));

        http_server = new org.eclipse.jetty.server.Server(thread_pool);

        ServerConnector http_connector = new ServerConnector(http_server);
        http_connector.setHost(config.server_host);
        http_connector.setPort(config.server_port);

        http_server.setConnectors(new Connector[]{http_connector});
        http_server.setHandler(new JettyHandler(jsonrpc_server));

        try {
            http_server.start();
        } catch (Exception ex) {
            System.err.println("Failed to start datastore: " + ex.getMessage());
        }
    }
}
