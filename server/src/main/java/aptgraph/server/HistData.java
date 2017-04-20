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
package aptgraph.server;

import java.util.LinkedList;
import java.util.TreeMap;

/**
 * Histogram data object.
 *
 * @author Thomas Gilon
 */
public class HistData extends TreeMap<Double, Double> {

    private String name = "";

    /**
     * Get HistData name.
     *
     * @return String : HistData name
     */
    public final String getName() {
        return name;
    }

    /**
     * Get HistData keys.
     *
     * @return LinkedList&lt;Double&gt; : HistData keys.
     */
    public final LinkedList<Double> getKeys() {
        LinkedList<Double> list = new LinkedList();
        for (Double i : this.keySet()) {
            list.add(i);
        }
        return list;
    }

    /**
     * Get HistData values.
     *
     * @return LinkedList&lt;Double&gt; : HistData values.
     */
    public final LinkedList<Double> getValues() {
        LinkedList<Double> list = new LinkedList();
        for (Double i : this.values()) {
            list.add(i);
        }
        return list;
    }

    /**
     * HistData serialization in array for Chartist library (see web site).
     *
     * @return String : Chartist format data
     */
    public final String getArray() {
        String data = "";
        LinkedList<Double> keys = this.getKeys();
        LinkedList<Double> values = this.getValues();
        for (int i = 0; i < keys.size(); i++) {
            data = data.concat("{&quot;x&quot;:");
            data = data.concat(keys.get(i).toString());
            data = data.concat(",&quot;y&quot;:");
            data = data.concat(values.get(i).toString());
            data = data.concat("}");
            if (i < keys.size() - 1) {
                data = data.concat(",");
            }
        }
        return "{&quot;data&quot;:{&quot;series&quot;:[["
                + data + "]]}}";
    }

    /**
     * Set HistData name.
     *
     * @param name HistData name
     */
    public final void setName(final String name) {
        this.name = name;
    }
}
