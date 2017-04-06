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
package aptgraph.config;

import java.math.BigDecimal;
import java.util.LinkedList;
import org.json.JSONObject;

/**
 *
 * @author Thomas Gilon
 */
public final class Sweep {
    private Sweep() {
    }

    /**
     * Create config lines, sweeping a given field in the config.
     * @param obj
     * @param field
     * @param start
     * @param stop
     * @param step
     * @param multi
     * @return LinkedList<String> config_lines_sweeped
     */
    public static LinkedList<String> sweepObj(
        final JSONObject obj,
        final String field,
        final BigDecimal start,
        final BigDecimal stop,
        final BigDecimal step,
        final String multi) {
        LinkedList<String> config_lines_sweeped = new LinkedList<String>();
        for (BigDecimal value = start; comparator(step, value, stop);
             value = value.add(step)) {
            JSONObject obj_new = new JSONObject(obj.toString());
            obj_new.put(field, value.toString());
            if (!multi.isEmpty()) {
                obj_new.put(multi, stop.subtract(value).toString());
            }
            String[] output_file_new_temp =
                    obj.getString("output_file").split("\\.");
            String output_file_new = output_file_new_temp[0];
            for (int i = 1; i < output_file_new_temp.length - 1; i += 1) {
               output_file_new =
                       output_file_new.concat("." + output_file_new_temp[i]);
            }
            output_file_new = output_file_new + "_" + field
                    + "_" + value.toString();
            if (!multi.isEmpty()) {
                output_file_new = output_file_new + "_" + multi
                    + "_" + stop.subtract(value).toString();
            }
            output_file_new = output_file_new + "."
                    + output_file_new_temp[output_file_new_temp.length - 1];
            obj_new.put("output_file", output_file_new);
            config_lines_sweeped.add(obj_new.toString());
        }
        return config_lines_sweeped;
    }

    /**
     * Comparator is adapted in function of the step.
     * @param step
     * @param value
     * @param stop
     * @return boolean
     */
    private static boolean comparator(
            final BigDecimal step,
            final BigDecimal value,
            final BigDecimal stop) {
        if (step.compareTo(BigDecimal.ZERO) >= 0)  {
            return value.compareTo(stop) <= 0;
        } else {
            return value.compareTo(stop) >= 0;
        }
    }
}
