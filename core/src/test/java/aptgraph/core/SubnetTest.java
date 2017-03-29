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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import junit.framework.TestCase;

/**
 *
 * @author Thomas Gilon
 */
public class SubnetTest extends TestCase {

     /**
     * Test subnet tools.
     */
    public void testSubnet() {
        System.out.println("Test Subnet");
        // Creation of the data
        ArrayList<String> users = new ArrayList<String>();
        try {
            Path input_dir = Paths.get("src/test/resources/dummyDir_users");
            File file = new File(input_dir.toString(), "users.ser");
            FileInputStream input_stream =
                    new FileInputStream(file.toString());
            ObjectInputStream input = new ObjectInputStream(
                    new BufferedInputStream(input_stream));
            users = (ArrayList<String>) input.readObject();
            input.close();
        } catch (IOException ex) {
                System.err.println(ex);
        } catch (ClassNotFoundException ex) {
                System.err.println(ex);
        }

        // Test isSubnet
        assertTrue(Subnet.isSubnet("192.168.1.0"));
        assertTrue(Subnet.isSubnet("192.0.1.0"));
        assertTrue(Subnet.isSubnet("192.168.0.0"));
        assertTrue(Subnet.isSubnet("192.0.0.0"));
        assertFalse(Subnet.isSubnet("192.168.1.1"));
        assertFalse(Subnet.isSubnet("192.0.1.1"));
        assertFalse(Subnet.isSubnet("192.0.0.1"));
        assertFalse(Subnet.isSubnet("0.0.0.1"));

        // Test getSubnet
        assertEquals(Subnet.getSubnet("192.168.1.0"), "192.168.1.");
        assertEquals(Subnet.getSubnet("192.0.1.0"), "192.0.1.");
        assertEquals(Subnet.getSubnet("0.0.1.0"), "0.0.1.");
        assertEquals(Subnet.getSubnet("192.168.0.0"), "192.168.");
        assertEquals(Subnet.getSubnet("0.168.0.0"), "0.168.");
        assertEquals(Subnet.getSubnet("192.0.0.0"), "192.");

        // Test getUsersInSubnet
        String input_1 = "198.0.0.0";
        String sn = Subnet.getSubnet(input_1);
        ArrayList<String> users_subnet = Subnet.getUsersInSubnet(input_1, users);
        for (String user : users) {
            if (user.startsWith(sn)) {
                assertTrue(users_subnet.contains(user));
            }
        }

        // Test getAllSubnets
        ArrayList<String> input = new ArrayList<String>();
        input.add("192.169.1.2");
        input.add("192.168.2.1");
        ArrayList<String> subnet_list = Subnet.getAllSubnets(input);
        // System.out.println("subnet_list = " + subnet_list);
        ArrayList<String> subnet_list_expected = new ArrayList<String>();
        subnet_list_expected.add("0.0.0.0");
        subnet_list_expected.add("192.0.0.0");
        subnet_list_expected.add("192.168.0.0");
        subnet_list_expected.add("192.168.2.0");
        subnet_list_expected.add("192.169.0.0");
        subnet_list_expected.add("192.169.1.0");
        assertEquals(subnet_list, subnet_list_expected);
    }    
}
