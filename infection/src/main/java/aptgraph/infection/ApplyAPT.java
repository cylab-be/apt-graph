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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TimeZone;

/**
 * Definition file for the application of APT in log files.
 *
 * @author Thomas Gilon
 */
public class ApplyAPT {

    private static final Logger LOGGER
            = Logger.getLogger(ApplyAPT.class.getName());

    /**
     * Infect a log file with a periodic APT.
     *
     * @param input_file File to infect
     * @param output_file Output file
     * @param apt_domain Domain of APT
     * @param user Targeted user
     * @param format File format (SQUID or JSON)
     * @param time_step Time step in ms between injections
     * @throws IOException If request can not be written
     */
    final void periodicAPT(
            final InputStream input_file,
            final OutputStream output_file,
            final String apt_domain,
            final String user,
            final String format,
            final long time_step)
            throws IOException {

        BufferedReader in = new BufferedReader(
                new InputStreamReader(input_file, "UTF-8"));
        BatchProcessor batch = new BatchProcessor();
        int counter_apt_total = 0;

        String line = in.readLine();
        Request request = batch.parseLine(line, format);
        Long time = request.getTime();
        output_file.write((line + "\n").getBytes("UTF-8"));

        while ((line = in.readLine()) != null) {
            request = batch.parseLine(line, format);

            while (time + time_step < request.getTime()) {
                writeRequest(buildAPT(time + time_step, apt_domain,
                        user), output_file, format);
                time = time + time_step;
                counter_apt_total += 1;
            }
            output_file.write((line + "\n").getBytes("UTF-8"));
        }

        LOGGER.log(Level.INFO, "Number of periodAPT injected : {0} ({1})",
                new Object[]{counter_apt_total, apt_domain});
    }

    /**
     * Infect a log file with an APT following the traffic pattern of a user.
     *
     * @param input_file File to infect
     * @param output_file Output file
     * @param apt_domain Domain of APT
     * @param user Targeted user
     * @param format File format (SQUID or JSON)
     * @param delta_time Time difference in ms between request to consider them
     * as part of a burst
     * @param duration Duration in ms expected of the burst of data to assess it
     * as burst
     * @param injection_day Number of injection allowed each day
     * @param proportion Proportion of APT effectively injected in all the
     * possible burst of data
     * @param delay Delay in ms between start of the burst and injection of APT
     * @throws IOException If request can not be written
     * @throws java.text.ParseException If file cannot be parsed
     */
    final void trafficAPT(
            final InputStream input_file,
            final OutputStream output_file,
            final String apt_domain,
            final String user,
            final String format,
            final long delta_time,
            final long duration,
            final int injection_day,
            final double proportion,
            final long distance_time,
            final long delay)
            throws IOException, ParseException {

        BufferedReader in = new BufferedReader(
                new InputStreamReader(input_file, "UTF-8"));
        BatchProcessor batch = new BatchProcessor();
        String line;
        int counter_apt_daily = 0; // number of effective daily injections
        int counter_apt_total_daily = 0; // number of potential daily injections
        int counter_apt_total = 0; // total number of injected APT
        long last_injection = 0; // Last injection

        String line_1 = in.readLine();
        Request request_1 = batch.parseLine(line_1, format);
        output_file.write((line_1 + "\n").getBytes("UTF-8"));
        // check if the request came from the targeted user
        while (!request_1.getClient().equals(user)
                && (line_1 = in.readLine()) != null) {
            request_1 = batch.parseLine(line_1, format);
            output_file.write((line_1 + "\n").getBytes("UTF-8"));
        }
        if (line_1 != null) {
            Long time = request_1.getTime();
            Long day = getDay(time);

            String line_2 = in.readLine();
            Request request_2 = batch.parseLine(line_2, format);
            output_file.write((line_2 + "\n").getBytes("UTF-8"));
            // check if the request came from the targeted user
            while (!request_2.getClient().equals(user)
                    && (line_2 = in.readLine()) != null) {
                request_2 = batch.parseLine(line_2, format);
                output_file.write((line_2 + "\n").getBytes("UTF-8"));
            }

            if (line_2 != null) {
                long[] out_1 = verifyTraffic(request_1, request_2,
                        day, delta_time, time, duration, counter_apt_daily,
                        counter_apt_total_daily, counter_apt_total,
                        injection_day, last_injection, proportion,
                        distance_time, delay,
                        apt_domain, user, output_file, format);
                time = out_1[0];
                day = out_1[1];
                last_injection = out_1[2];
                counter_apt_daily = (int) out_1[3];
                counter_apt_total_daily = (int) out_1[4];
                counter_apt_total = (int) out_1[5];

                while ((line = in.readLine()) != null) {
                    request_1 = request_2;
                    request_2 = batch.parseLine(line, format);
                    output_file.write((line + "\n").getBytes("UTF-8"));
                    while (!request_2.getClient().equals(user)
                            && (line = in.readLine()) != null) {
                        request_2 = batch.parseLine(line, format);
                        output_file.write((line + "\n").getBytes("UTF-8"));
                    }

                    if (line != null) {
                        long[] out_2 = verifyTraffic(request_1, request_2,
                                day, delta_time, time, duration, counter_apt_daily,
                                counter_apt_total_daily, counter_apt_total,
                                injection_day, last_injection, proportion,
                                distance_time, delay,
                                apt_domain, user, output_file, format);
                        time = out_2[0];
                        day = out_2[1];
                        last_injection = out_2[2];
                        counter_apt_daily = (int) out_2[3];
                        counter_apt_total_daily = (int) out_2[4];
                        counter_apt_total = (int) out_2[5];
                    }
                }
            }
        }

        LOGGER.log(Level.INFO, "Number of trafficAPT injected : {0} ({1})",
                new Object[]{counter_apt_total, apt_domain});
    }

    /**
     * Verify the traffic between two requests to search for burst of data and
     * inject APT if needed.
     *
     * @param request_1 Request 1
     * @param request_2 Request 2
     * @param day_in UNIX timestamp of actual day in ms
     * @param delta_time Time difference in ms between request to consider them
     * as part of a burst
     * @param time_in Time of the start of the burst of data in ms
     * @param duration Duration in ms expected of the burst of data to assess it
     * as burst
     * @param counter_apt_daily_in Number of APT already injected during the day
     * @param counter_apt_total_daily_in Number of APT that could already be
     * injected during the day if a proportion of 1 has been given
     * @param counter_apt_total_in Number of APT already injected in the file
     * @param injection_day Number of injection allowed each day
     * @param last_injection_in UNIX timestamp of the last injection
     * @param proportion Proportion of APT effectively injected in all the
     * possible burst of data
     * @param distance_time Minimal duration between two injection in ms
     * @param delay Delay in ms between start of the burst and injection of APT
     * @param apt_domain Domain of APT
     * @param user Targeted user
     * @param output_file Output file
     * @param format File format (SQUID or JSON)
     * @return long[] : Update of variables {time, last_injection,
     * counter_apt_daily, counter_apt_total_daily, counter_apt_total}
     * @throws ParseException If expression can not be parsed
     * @throws IOException If request can not be written
     */
    private long[] verifyTraffic(final Request request_1,
            final Request request_2, final long day_in, final long delta_time,
            final long time_in, final long duration,
            final int counter_apt_daily_in,
            final int counter_apt_total_daily_in,
            final int counter_apt_total_in,
            final int injection_day, final long last_injection_in,
            final double proportion, final long distance_time, final long delay,
            final String apt_domain, final String user,
            final OutputStream output_file, final String format)
            throws ParseException, IOException {
        long time = time_in;
        long day = day_in;
        long last_injection = last_injection_in;
        int counter_apt_daily = counter_apt_daily_in;
        int counter_apt_total_daily = counter_apt_total_daily_in;
        int counter_apt_total = counter_apt_total_in;

        if (day == getDay(request_2.getTime())) {
            // Same day
            Long delta_time_requests = request_2.getTime()
                    - request_1.getTime();
            if (delta_time_requests <= delta_time) {
                // The second request is close enough
                if (request_2.getTime() - time >= duration
                        && last_injection != time
                        && (last_injection + distance_time
                        <= request_2.getTime())) {
                    // Traffic is dense enough and no APT have
                    // been inject in this burst or too shortly
                    if (counter_apt_total_daily < injection_day) {
                        // APT could be insert but number of injection
                        // is limited by proportion
                        counter_apt_total_daily += 1;
                        last_injection = time;
                        if (Math.abs((double) (counter_apt_daily + 1)
                                / (double) counter_apt_total_daily - proportion)
                                < 1E-10) {
                            // Proportion is respected and APT is injected
                            writeRequest(buildAPT(time + delay,
                                    apt_domain, user), output_file, format);
                            counter_apt_daily += 1;
                            counter_apt_total += 1;
                        }
                    }
                }
            } else {
                time = request_2.getTime();
            }
        } else {
            time = request_2.getTime();
            day = getDay(time);
            counter_apt_daily = 0;
            counter_apt_total_daily = 0;
        }
        long[] output = {time, day, last_injection,
            (long) counter_apt_daily, (long) counter_apt_total_daily,
            (long) counter_apt_total};
        return output;
    }

    /**
     * Write the given request to the output file.
     *
     * @param request Request to write
     * @param output_file Output file
     * @param format File format (SQUID or JSON)
     * @throws IOException If request can not be written
     */
    final void writeRequest(final Request request,
            final OutputStream output_file, final String format)
            throws IOException {
        String request_string = "";
        if (format.equals("squid")) {
            request_string = String.valueOf(request.getTime()).substring(0, 10)
                    + "." + String.valueOf(request.getTime()).substring(10, 13)
                    + "    " + request.getElapsed() + " "
                    + request.getClient() + " " + request.getCode() + "/"
                    + request.getStatus() + " " + request.getBytes() + " "
                    + request.getMethod() + " " + request.getUrl() + " - "
                    + request.getPeerstatus() + "/" + request.getPeerhost()
                    + " " + request.getType() + "\n";
        } else if (format.equals("json")) {
            SimpleDateFormat formatter
                    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String time_string = formatter.format(new Date(request.getTime()));
            request_string = "{\"@version\":\"0\","
                    + "\"@timestamp\":\"" + time_string + "\","
                    + "\"type\":\"0\","
                    + "\"timestamp\":\"0\","
                    + "\"tk_username\":\"" + request.getClient() + "\","
                    + "\"tk_url\":\"" + request.getUrl() + "\","
                    + "\"tk_size\":" + request.getBytes() + ","
                    + "\"tk_date_field\":\"0\","
                    + "\"tk_protocol\":\"0\","
                    + "\"tk_mime_content\":\"" + request.getType() + "\","
                    + "\"tk_client_ip\":\"" + request.getClient() + "\","
                    + "\"tk_server_ip\":\"" + request.getPeerhost() + "\","
                    + "\"tk_domain\":\"0\","
                    + "\"tk_path\":\"0\","
                    + "\"tk_operation\":\"" + request.getMethod() + "\","
                    + "\"tk_uid\":\"0\","
                    + "\"tk_category\":\"0\","
                    + "\"tk_category_type\":\"0\","
                    + "\"geoip\":{\"ip\":\"0\","
                    + "\"country_code2\":\"0\","
                    + "\"country_code3\":\"0\","
                    + "\"country_name\":\"0\","
                    + "\"continent_code\":\"0\","
                    + "\"latitude\":0,"
                    + "\"longitude\":0,"
                    + "\"timezone\":\"0\","
                    + "\"location\":[0,0]},"
                    + "\"category\":\"0\"}" + "\n";
        }

        output_file.write(request_string.getBytes("UTF-8"));
    }

    /**
     * Build up a fake request for the APT.
     *
     * @param time Time of the request
     * @param apt_domain Domain of APT
     * @param user Targeted user
     * @return Request : Request of the APT
     */
    final Request buildAPT(final long time, final String apt_domain,
            final String user) {
        Request request = new Request(
                time, 10, user, "TCP_APT", 200, 10, "GET", "http://" + apt_domain,
                "-", "HIER_DIRECT", "167.167.167.167", "text/html");
        return request;
    }

    /**
     * Give the UNIX timestamp of midnight of the current day.
     *
     * @param time UNIX timestamp
     * @return long : UNIX timestamp of midnight of the current day
     * @throws ParseException If expression can not be parsed
     */
    final long getDay(final long time) throws ParseException {
        SimpleDateFormat formatter
                = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time_string = formatter.format(new Date(time * 1L));
        // System.out.println("time_string = " + time_string);
        String[] time_split = time_string.split(" ");
        String day_string = time_split[0] + " 00:00:00.000";
        // System.out.println("day_string = " + day_string);
        // System.out.println(formatter.parse(day_string).getTime());
        return formatter.parse(day_string).getTime();
    }
}
