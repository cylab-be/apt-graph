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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 *
 * @author Thomas Gilon
 */
public class DomainTest {

    /**
     * Test the merge of two domains.
     */
    public void testMerge() {
        System.out.println("Test Merge");

        //Create data
        Request req_1 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_2 = new Request(
                (long) 1486934545,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_3 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_4 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://i.be/", (String) "i.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");

        Domain dom_1 = new Domain();
        dom_1.setName("j.be");
        dom_1.add(req_1);
        dom_1.add(req_2);
        // System.out.println("dom_1 = " + Arrays.toString(dom_1.toArray()));

        Domain dom_2 = new Domain();
        dom_2.setName("j.be");
        dom_2.add(req_3);
        // System.out.println("dom_2 = " + Arrays.toString(dom_2.toArray()));

        Domain dom_3 = new Domain();
        dom_3.setName("i.be");
        dom_3.add(req_4);
        // System.out.println("dom_3 = " + Arrays.toString(dom_3.toArray()));

        Domain merge_1 = dom_1.merge(dom_2);
        Domain merge_2 = dom_2.merge(dom_2);
        Domain merge_3 = dom_2.merge(dom_3);
        // System.out.println("merge_1 = " + Arrays.toString(merge_1.toArray()));
        // System.out.println("merge_2 = " + Arrays.toString(merge_2.toArray()));
        // System.out.println("merge_3 = " + Arrays.toString(merge_3.toArray()));

        // Test
        assertTrue(merge_1.contains(req_1));
        assertTrue(merge_1.contains(req_2));
        assertTrue(merge_1.contains(req_3));
        assertTrue(merge_1.size() == 3);

        assertTrue(merge_2.contains(req_3));
        assertTrue(merge_2.size() == 1);

        assertTrue(merge_3.contains(req_3));
        assertFalse(merge_3.contains(req_4));
        assertTrue(merge_3.size() == 1);
    }

    public void testCompareTo() {
        System.out.println("Test CompareTo");

        //Create data
        Request req_1 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_2 = new Request(
                (long) 1486934545,
                (int) 51, (String) "127.0.0.1", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_3 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://j.be/", (String) "j.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");
        Request req_4 = new Request(
                (long) 1486934544,
                (int) 51, (String) "127.0.0.2", (String) "TCP_MISS",
                (int) 200, (int) 4575, (String) "GET",
                (String) "http://i.be/", (String) "i.be",
                (String) "HIER_DIRECT", (String) "95.101.90.153", (String) "-");

        Domain dom_1 = new Domain();
        dom_1.setName("j.be");
        dom_1.add(req_1);
        dom_1.add(req_2);

        Domain dom_2 = new Domain();
        dom_2.setName("j.be");
        dom_2.add(req_1);
        dom_2.add(req_2);

        Domain dom_3 = new Domain();
        dom_3.setName("j.be");
        dom_3.add(req_1);

        Domain dom_4 = new Domain();
        dom_4.setName("j.be");
        dom_4.add(req_1);
        dom_4.add(req_2);
        dom_4.add(req_3);

        Domain dom_5 = new Domain();
        dom_5.setName("j.be");
        dom_5.add(req_1);
        dom_5.add(req_3);

        Domain dom_6 = new Domain();
        dom_6.setName("i.be");
        dom_6.add(req_1);
        dom_6.add(req_2);

        // Test
        assertTrue(dom_1.deepEquals(dom_2));
        assertFalse(dom_1.deepEquals(dom_3));
        assertFalse(dom_1.deepEquals(dom_4));
        assertFalse(dom_1.deepEquals(dom_5));
        assertFalse(dom_1.deepEquals(dom_6));
    }
    
}
