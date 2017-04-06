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
 *
 * @author Thomas Gilon
 */
public class ApplyAPT {

    private static final Logger LOGGER
            = Logger.getLogger(ApplyAPT.class.getName());

    /**
     * Infect a log file with a periodic APT.
     * @param input_file
     * @param output_file
     * @param apt_domain
     * @param user
     * @param format
     * @param time_step
     * @throws IOException
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

            if (time + time_step < request.getTime()) {
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
     * @param input_file
     * @param output_file
     * @param apt_domain
     * @param user
     * @param format
     * @param delta_time
     * @param duration
     * @param injection_day
     * @param proportion
     * @param delay
     * @throws IOException
     * @throws java.text.ParseException
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
        Long time = request_1.getTime();
        Long day = getDay(time);

        String line_2 = in.readLine();
        Request request_2 = batch.parseLine(line_2, format);
        output_file.write((line_2 + "\n").getBytes("UTF-8"));

        long[] out_1 = verifyTraffic(request_1, request_2,
                day, delta_time, time, duration, counter_apt_daily,
                counter_apt_total_daily, counter_apt_total,
                injection_day, last_injection, proportion, distance_time, delay,
                apt_domain, user, output_file, format);
        time = out_1[0];
        last_injection = out_1[1];
        counter_apt_daily = (int) out_1[2];
        counter_apt_total_daily = (int) out_1[3];
        counter_apt_total = (int) out_1[4];

        while ((line = in.readLine()) != null) {
            request_1 = request_2;
            request_2 = batch.parseLine(line, format);
            output_file.write((line + "\n").getBytes("UTF-8"));

            long[] out_2 = verifyTraffic(request_1, request_2,
                day, delta_time, time, duration, counter_apt_daily,
                counter_apt_total_daily, counter_apt_total,
                injection_day, last_injection, proportion, distance_time, delay,
                apt_domain, user, output_file, format);
            time = out_2[0];
            last_injection = out_2[1];
            counter_apt_daily = (int) out_2[2];
            counter_apt_total_daily = (int) out_2[3];
            counter_apt_total = (int) out_2[4];
        }

        LOGGER.log(Level.INFO, "Number of periodAPT injected : {0} ({1})",
                new Object[]{counter_apt_total, apt_domain});
    }

    /**
     * Verify the traffic and inject APT if needed.
     * @param request_1
     * @param request_2
     * @param day
     * @param delta_time
     * @param time_in
     * @param duration
     * @param counter_apt_daily_in
     * @param counter_apt_total_daily_in
     * @param counter_apt_total_in
     * @param injection_day
     * @param last_injection_in
     * @param proportion
     * @param distance_time
     * @param delay
     * @param apt_domain
     * @param user
     * @param output_file
     * @param format
     * @return long[] {time, last_injection, counter_apt_daily,
     * counter_apt_total_daily, counter_apt_total}
     * @throws ParseException
     * @throws IOException
     */
    private long[] verifyTraffic(final Request request_1,
            final Request request_2, final long day, final long delta_time,
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
            counter_apt_daily = 0;
            counter_apt_total_daily = 0;
        }
        long[] output = {time, last_injection,
            (long) counter_apt_daily, (long) counter_apt_total_daily,
            (long) counter_apt_total};
        return output;
    }

    /**
     * Write the given request to the output file.
     * @param request
     * @param output_file
     * @param format
     * @throws IOException
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
                = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String time_string = formatter.format(new Date(request.getTime()));
            String[] time_string_split = time_string.split(" ");
            time_string = time_string_split[0] + "T"
                    + time_string_split[1] + "Z";
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
     * @param time
     * @param apt_domain
     * @param user
     * @return Request
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
     * @param time
     * @return long
     * @throws ParseException
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
