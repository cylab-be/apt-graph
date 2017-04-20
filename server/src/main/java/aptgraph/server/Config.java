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

/**
 * Configuration file for server.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
 */
public class Config {

    private static final int DEFAULT_MAX_THREADS = 40;
    private static final int DEFAULT_MIN_THREADS = 10;
    private static final int DEFAULT_IDLE_TIMEOUT = 60;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final int DEFAULT_MAX_PENDING_REQUESTS = 300;

    // Datastore HTTP/JSON-RPC server parameters
    private int max_threads = DEFAULT_MAX_THREADS;
    private int min_threads = DEFAULT_MIN_THREADS;
    private int idle_timeout = DEFAULT_IDLE_TIMEOUT;
    private String server_host = DEFAULT_SERVER_HOST;
    private int server_port = DEFAULT_SERVER_PORT;
    private int max_pending_requests = DEFAULT_MAX_PENDING_REQUESTS;

    /**
     * Get maximum number of threads.
     *
     * @return int : Maximum number of threads
     */
    public final int getMaxThreads() {
        return max_threads;
    }

    /**
     * Set maximum number of threads.
     *
     * @param max_threads Maximum number of threads
     */
    public final void setMaxThreads(final int max_threads) {
        this.max_threads = max_threads;
    }

    /**
     * Get minimum number of threads.
     *
     * @return int : Minimum number of threads
     */
    public final int getMinThreads() {
        return min_threads;
    }

    /**
     * Set minimum number of threads.
     *
     * @param min_threads Minimum number of threads
     */
    public final void setMinThreads(final int min_threads) {
        this.min_threads = min_threads;
    }

    /**
     * Get idle time out.
     *
     * @return int : Idle time out
     */
    public final int getIdleTimeout() {
        return idle_timeout;
    }

    /**
     * Get idle time out.
     *
     * @param idle_timeout Idle time out
     */
    public final void setIdleTimeout(final int idle_timeout) {
        this.idle_timeout = idle_timeout;
    }

    /**
     * Get server host.
     *
     * @return String : Server host
     */
    public final String getServerHost() {
        return server_host;
    }

    /**
     * Set server host.
     *
     * @param server_host Server host
     */
    public final void setServerHost(final String server_host) {
        this.server_host = server_host;
    }

    /**
     * Get server port.
     *
     * @return int : Server port
     */
    public final int getServerPort() {
        return server_port;
    }

    /**
     * Set server port.
     *
     * @param server_port Server port
     */
    public final void setServerPort(final int server_port) {
        this.server_port = server_port;
    }

    /**
     * Get maximum pending requests.
     *
     * @return int : Maximum pending requests
     */
    public final int getMaxPendingRequests() {
        return max_pending_requests;
    }

    /**
     * Set maximum pending requests.
     *
     * @param max_pending_requests Maximum pending requests
     */
    public final void setMaxPendingRequests(final int max_pending_requests) {
        this.max_pending_requests = max_pending_requests;
    }

    @Override
    public final String toString() {
        return "Config with port " + this.server_port;
    }
}
