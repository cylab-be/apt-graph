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

import java.util.ArrayList;

/**
 *
 * @author Thomas Gilon
 */
public class Subnet {
    // This is only compatible with IPv4

    /**
     * Give the list of users in a specific subnet.
     * @param input
     * @param all_users
     * @return users_subnet
     */
    public final ArrayList<String> getUsersInSubnet(
            final String input, final ArrayList<String> all_users) {
        ArrayList<String> users_subnet = new ArrayList<String>();
        String subnet = getSubnet(input);
        for (String ip : all_users) {
            if (ip.startsWith(subnet)) {
                users_subnet.add(ip);
            }
        }
        return users_subnet;
    }

    /**
     * Compute all the possible subnets based on the given users list.
     * @param all_users
     * @return subnet_list
     */
    public final ArrayList<String> getAllSubnets(
            final ArrayList<String> all_users) {
        ArrayList<String> subnet_list = new ArrayList<String>();
        subnet_list.add("0.0.0.0");
        for (String user : all_users) {
            String subnet = "";
            String[] split = user.split("[.]");
            for (int i = 0; i < split.length - 1; i++) {
                subnet = subnet.concat(split[i]);
                subnet = subnet.concat(".");
                String subnet_temp = subnet;
                for (int j = 0; j < 3 - i; j++) {
                    subnet_temp = subnet_temp.concat("0");
                    if (j < 3 - i - 1) {
                        subnet_temp = subnet_temp.concat(".");
                    }
                }
                if (!subnet_list.contains(subnet_temp)) {
                    subnet_list.add(subnet_temp);
                }
            }
        }
        subnet_list = sortIPs(subnet_list);
        return subnet_list;
    }

    /**
     * Simple method to sort IPv4 list.
     * @param ip_list
     * @return ArrayList<String>
     */
    public final ArrayList<String> sortIPs(final ArrayList<String> ip_list) {
        ArrayList<String> sorted_ip_list = new ArrayList<String>();
        ArrayList<String> ip_list_temp = new ArrayList<String>(ip_list);
        String selected;
        String[] selected_split;
        while (!ip_list_temp.isEmpty()) {
            selected = ip_list_temp.get(0);
            selected_split = selected.split("[.]");
            for (String ip : ip_list_temp) {
                String[] split = ip.split("[.]");

                if ((Integer.parseInt(split[0])
                        < Integer.parseInt(selected_split[0]))

                    || (Integer.parseInt(split[0])
                        == Integer.parseInt(selected_split[0])
                        && Integer.parseInt(split[1])
                            < Integer.parseInt(selected_split[1]))

                    || (Integer.parseInt(split[0])
                        == Integer.parseInt(selected_split[0])
                        && Integer.parseInt(split[1])
                            == Integer.parseInt(selected_split[1])
                        && Integer.parseInt(split[2])
                            < Integer.parseInt(selected_split[2]))

                    || (Integer.parseInt(split[0])
                        == Integer.parseInt(selected_split[0])
                        && Integer.parseInt(split[1])
                            == Integer.parseInt(selected_split[1])
                        && Integer.parseInt(split[2])
                            == Integer.parseInt(selected_split[2])
                        && Integer.parseInt(split[3])
                                < Integer.parseInt(selected_split[3]))) {
                                selected = ip;
                                selected_split = selected.split("[.]");
                }
            }
            sorted_ip_list.add(selected);
            ip_list_temp.remove(selected);
        }
        return sorted_ip_list;
    }

    /**
     * Compute the subnet based on the string given by user.
     * @param input
     * @return subnet
     */
    public final String getSubnet(final String input) {
        String subnet = ".";
        String[] split = input.split("[.]");
        boolean found_end = false;
        for (int i = split.length - 1; i >= 0; i--) {
            if (found_end || !split[i].equals("0")) {
                subnet = split[i].concat(subnet);
                found_end = true;
                if (i != 0) {
                    subnet = ".".concat(subnet);
                }
            }
        }
        return subnet;
    }

    /**
     * Verify if the user given is a subnet.
     * @param user
     * @return boolean
     */
    public final boolean isSubnet(final String user) {
        boolean bool = false;
        for (String ip_split : user.split("[.]")) {
            if (ip_split.equals("0")) {
                bool = true;
            } else {
                bool = false;
            }
        }
        return bool;
    }
}
