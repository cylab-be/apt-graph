/*
 * The MIT License
 *
 * Copyright 2017 Thomas.
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

import info.debatty.java.graphs.Graph;
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 * @author Thomas
 */
public class Output implements Serializable {
    private String name = "";
    private LinkedList<Graph<Domain>> filtered;
    private String stdout;

    /**
     * Return name.
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * Return filtered.
     * @return
     */
    public final LinkedList<Graph<Domain>> getFiltered() {
        return filtered;
    }

    /**
     * Return standard output.
     * @return
     */
    public final String getStdout() {
        return stdout;
    }

    /**
     * Set output name.
     * @param name
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * Set filtered.
     * @param filtered
     */
    public final void setFiltered(final LinkedList<Graph<Domain>> filtered) {
        this.filtered = filtered;
    }

    /**
     * Set stdout.
     * @param stdout
     */
    public final void setStdout(final String stdout) {
        this.stdout = stdout;
    }

    /**
     * Return ReturnAnalyze name.
     * @return
     */
    @Override
    public final String toString() {
        return name;
    }
}

