/*
 * The MIT License
 *
 * Copyright 2017 Thomas Gilon.
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
package aptgraph.infection;

import aptgraph.batch.BatchProcessor;
import aptgraph.core.Request;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import junit.framework.TestCase;

/**
 *
 * @author Thomas Gilon
 */
public class ApplyTest extends TestCase {

    /**
     * Test of periodicAPT() method.
     * @throws IOException 
     */
    public final void testPeriodicAPT() throws IOException {
        System.out.println("Test periodicAPT()");
        File temp_file = File.createTempFile("file", ".log");

        ApplyAPT apt = new ApplyAPT();
        apt.periodicAPT(getClass().getResourceAsStream(
                "/1000_http_requests.txt"),
                new FileOutputStream(temp_file),
                "APT.FINDME.be", "167.167.167.167", "squid", 1000L);

        BufferedReader in_1 = new BufferedReader(
                new InputStreamReader(new FileInputStream(temp_file), "UTF-8"));
        BufferedReader in_2 = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(
                "/1000_http_requests.txt"), "UTF-8"));

        String line_1;
        Request request_1;
        String line_2;
        Request request_2;
        BatchProcessor batch = new BatchProcessor();
        while ((line_1 = in_1.readLine()) != null
                && (line_2 = in_2.readLine()) != null) {
            request_1 = batch.parseLine(line_1, "squid");
            request_2 = batch.parseLine(line_2,"squid");
            if (request_1.getDomain().equals("APT.FINDME.be")) {
                line_1 = in_1.readLine();
                request_1 = batch.parseLine(line_1, "squid");
            }

            assertEquals(line_1, line_2);
            assertEquals(request_1, request_2);
        }
    }

    /**
     * Test of trafficAPT() method.
     * @throws IOException 
     * @throws java.text.ParseException 
     */
    public final void testTrafficAPT() throws IOException, ParseException {
        System.out.println("Test periodicAPT()");
        File temp_file = File.createTempFile("file", ".log");

        ApplyAPT apt = new ApplyAPT();
        apt.trafficAPT(getClass().getResourceAsStream(
                "/1000_http_requests.txt"),
                new FileOutputStream(temp_file),
                "APT.FINDME.be", "167.167.167.167", "squid", 20, 100L, 10, 0.5,
                1800000, 50L);

        BufferedReader in_1 = new BufferedReader(
                new InputStreamReader(new FileInputStream(temp_file), "UTF-8"));
        BufferedReader in_2 = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(
                "/1000_http_requests.txt"), "UTF-8"));

        String line_1;
        Request request_1;
        String line_2;
        Request request_2;
        BatchProcessor batch = new BatchProcessor();
        while ((line_1 = in_1.readLine()) != null
                && (line_2 = in_2.readLine()) != null) {
            request_1 = batch.parseLine(line_1, "squid");
            request_2 = batch.parseLine(line_2,"squid");
            if (request_1.getDomain().equals("APT.FINDME.be")) {
                line_1 = in_1.readLine();
                request_1 = batch.parseLine(line_1, "squid");
            }

            assertEquals(line_1, line_2);
            assertEquals(request_1, request_2);
        }
    }

    /**
     * Test generation of the UNIX day timestamp.
     * @throws ParseException 
     */
    public final void testGetDay() throws ParseException {
        System.out.println("Test getDay()");

        ApplyAPT apt = new ApplyAPT();
        assertTrue(apt.getDay(1424217600000L) == 1424217600000L);
        assertTrue(apt.getDay(1424246271229L) == 1424217600000L);
    }

    /**
     * Test generation of APT log entry.
     * @throws IOException 
     */
    public final void testBuildAPT() throws IOException {
        System.out.println("Test buildAPT()");

        ApplyAPT apt = new ApplyAPT();
        Request apt_request = apt.buildAPT(1424246271229L, "APT.FINDME.be",
                "127.0.0.1");
        BatchProcessor batch = new BatchProcessor();

        // Squid format
        File temp_file_squid = File.createTempFile("file_squid", ".log");
        BufferedReader in_squid = new BufferedReader(
                new InputStreamReader(new FileInputStream(temp_file_squid), "UTF-8"));

        String squid_request_string = "1424246271.229    10 127.0.0.1"
                + " TCP_APT/200 10 GET http://APT.FINDME.be - HIER_DIRECT/"
                + "167.167.167.167 text/html";
        Request squid_request = batch.parseLine(squid_request_string, "squid");

        apt.writeRequest(apt_request, new FileOutputStream(temp_file_squid), "squid");
        String apt_string_squid = in_squid.readLine();

        assertEquals(apt_string_squid, squid_request_string); // compare string
        assertEquals(apt_request, squid_request); // compare requests

        // JSON format
        File temp_file_json = File.createTempFile("file_json", ".log");
        BufferedReader in_json = new BufferedReader(
                new InputStreamReader(new FileInputStream(temp_file_json), "UTF-8"));

        String json_request_string = "{\"@version\":\"0\",\"@timestamp\":\"2015-02-18T08:57:51.229Z\",\"type\":\"0\",\"timestamp\":\"0\",\"tk_username\":\"127.0.0.1\",\"tk_url\":\"http://APT.FINDME.be\",\"tk_size\":10,\"tk_date_field\":\"0\",\"tk_protocol\":\"0\",\"tk_mime_content\":\"text/html\",\"tk_client_ip\":\"127.0.0.1\",\"tk_server_ip\":\"167.167.167.167\",\"tk_domain\":\"0\",\"tk_path\":\"0\",\"tk_operation\":\"GET\",\"tk_uid\":\"0\",\"tk_category\":\"0\",\"tk_category_type\":\"0\",\"geoip\":{\"ip\":\"0\",\"country_code2\":\"0\",\"country_code3\":\"0\",\"country_name\":\"0\",\"continent_code\":\"0\",\"latitude\":0,\"longitude\":0,\"timezone\":\"0\",\"location\":[0,0]},\"category\":\"0\"}";
        Request json_request = batch.parseLine(json_request_string, "json");

        apt.writeRequest(apt_request, new FileOutputStream(temp_file_json), "json");
        String apt_string_json = in_json.readLine();

        assertEquals(apt_string_json, json_request_string); // compare string
        assertEquals(apt_request, json_request); // compare requests
    }
}
