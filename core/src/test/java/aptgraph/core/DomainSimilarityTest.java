/*
 * The MIT License
 *
 * Copyright 2016 Thomas Gilon.
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
package aptgraph.core;

import static junit.framework.Assert.assertTrue;

/**
 *
 * @author Thomas Gilon
 */
public class DomainSimilarityTest {

    /**
     * Test Computation of Domain Similarity.
     */
    public void testDomainSimilarity() {
        System.out.println("Test Domain Similarity");

        //Create data
        Request req_1 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://a.be/", (String) "a.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_2 = new Request(
                (long) 1486934545,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://a.be/", (String) "a.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_3 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://b.a.be/", (String) "b.a.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_4 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://i.be/", (String) "i.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_5 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://c.b.a.be/", (String) "c.b.a.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_6 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://c.b.d.be/", (String) "c.b.d.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_7 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://e.b.d.be/", (String) "e.b.d.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_8 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://e.b.d.be/", (String) "e.b.d.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_9 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://domain/", (String) "domain",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_10 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://domain/", (String) "domain",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_11 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://other/", (String) "other",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");

        DomainSimilarity sim = new DomainSimilarity();
        assertTrue(sim.similarity(req_1, req_2) == 1.0);
        assertTrue(sim.similarity(req_1, req_3) == 1.0/2);
        assertTrue(sim.similarity(req_1, req_4) == 0.0);
        assertTrue(sim.similarity(req_1, req_5) == 1.0/3);
        assertTrue(sim.similarity(req_1, req_6) == 0.0);
        assertTrue(sim.similarity(req_6, req_7) == 2.0/3);
        assertTrue(sim.similarity(req_7, req_8) == 1.0);
        assertTrue(sim.similarity(req_9, req_10) == 1.0);
        assertTrue(sim.similarity(req_9, req_11) == 0.0);
    }
    
}
