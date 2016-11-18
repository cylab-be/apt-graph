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

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 *
 * @author Thibault Debatty
 */
public class ServerTest extends TestCase {

    private static final int STARTUP_DELAY = 5000;
    private volatile Exception server_thread_ex = null;
    /**
     * Test of start method, of class Server.
     * @throws java.lang.InterruptedException
     */
    public final void testStart() throws InterruptedException, Exception {
        System.out.println("start");
        final JsonRpcServer server = new JsonRpcServer(
                getClass().getResourceAsStream("/dummy_graph.ser"));

        Config conf = new Config();
        conf.setServerPort(12345);
        server.setConfig(conf);

        // Start the server in a separate thread, so we can wait and stop it
        // from the main thread...
        Thread server_thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    server.start();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, ex);
                    server_thread_ex = ex;

                } catch (Exception ex) {
                    Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, ex);
                    server_thread_ex = ex;
                }
            }
        });

        server_thread.start();

        // wait a little...
        Thread.sleep(STARTUP_DELAY);

        // Ask the server to stop
        server.stop();

        // Wait for the server to finish...
        server_thread.join();

        // Throw the exception in the main thread, to mark the test as failed
        if (server_thread_ex != null) {
            throw server_thread_ex;
        }
    }

}
