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
 *
 * @author Thibault Debatty
 */
public class Config {

    private static final int    DEFAULT_MAX_THREADS = 40;
    private static final int    DEFAULT_MIN_THREADS = 10;
    private static final int    DEFAULT_IDLE_TIMEOUT = 60;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int    DEFAULT_SERVER_PORT = 8080;
    private static final int    DEFAULT_MAX_PENDING_REQUESTS = 300;


    // Datastore HTTP/JSON-RPC server parameters
    public int max_threads = DEFAULT_MAX_THREADS;
    public int min_threads = DEFAULT_MIN_THREADS;
    public int idle_timeout = DEFAULT_IDLE_TIMEOUT;
    public String server_host = DEFAULT_SERVER_HOST;
    public int server_port = DEFAULT_SERVER_PORT;
    public int max_pending_requests = DEFAULT_MAX_PENDING_REQUESTS;

    @Override
    public String toString() {
        return "Config with port " + this.server_port;
    }
}
