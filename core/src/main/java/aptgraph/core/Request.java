/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty & Thomas Gilon.
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

import java.io.Serializable;
import net.jcip.annotations.Immutable;

/**
 * Represents a single Request. The class is immutable (once it is created, it
 * cannot be modified anymore), which is very handy for multi-threaded
 * programming.
 *
 * @author Thibault Debatty
 * @author Thomas Gilon
 */
@Immutable
public class Request implements Serializable {

    /**
     * Request time in ms.
     */
    private final long time;
    private final int elapsed;
    private final String client;
    private final String code;
    private final int status;
    private final int bytes;
    private final String method;
    private final String url;
    private final String domain;
    private final String peerstatus;
    private final String peerhost;
    private final String type;

    /**
     * Create a new immutable request (see
     * http://wiki.squid-cache.org/Features/LogFormat for futher details about
     * format).
     *
     * @param time Unix timestamp as UTC ms
     * @param elapsed Elapsed time in ms
     * @param client IP address of the requesting instance
     * @param code Squid result code
     * @param status Status code
     * @param bytes Size
     * @param method Request method
     * @param url URL requested
     * @param domain Domain name
     * @param peerstatus Hierarchy codes
     * @param peerhost IP address where the request was forwarded
     * @param type Content type
     */
    public Request(
            final long time,
            final int elapsed,
            final String client,
            final String code,
            final int status,
            final int bytes,
            final String method,
            final String url,
            final String domain,
            final String peerstatus,
            final String peerhost,
            final String type) {

        this.time = time;
        this.elapsed = elapsed;
        this.client = client;
        this.code = code;
        this.status = status;
        this.bytes = bytes;
        this.method = method;
        this.url = url;
        this.domain = domain;
        this.peerstatus = peerstatus;
        this.peerhost = peerhost;
        this.type = type;
    }

    /**
     * Get Unix Timestamp as UTC milliseconds.
     *
     * @return long : Unix timestamp as UTC ms
     */
    public final long getTime() {
        return time;
    }

    /**
     * Get elapsed time in millisecond.
     *
     * @return int : Elapsed time in ms
     */
    public final int getElapsed() {
        return elapsed;
    }

    /**
     * Get IP address of the requesting instance.
     *
     * @return String : IP address of the requesting instance
     */
    public final String getClient() {
        return client;
    }

    /**
     * Get squid result code.
     *
     * @return String : Squid result code
     */
    public final String getCode() {
        return code;
    }

    /**
     * Get status code.
     *
     * @return int : Status code
     */
    public final int getStatus() {
        return status;
    }

    /**
     * Get size.
     *
     * @return int : Size
     */
    public final int getBytes() {
        return bytes;
    }

    /**
     * Get request method.
     *
     * @return String : Request method
     */
    public final String getMethod() {
        return method;
    }

    /**
     * Get URL requested.
     *
     * @return url : URL requested
     */
    public final String getUrl() {
        return url;
    }

    /**
     * Get domain name.
     *
     * @return String : Domain name
     */
    public final String getDomain() {
        return domain;
    }

    /**
     * Get hierarchy codes.
     *
     * @return peerstatus : Hierarchy codes
     */
    public final String getPeerstatus() {
        return peerstatus;
    }

    /**
     * Get IP address where the request was forwarded, this is the origin IP of
     * the server if peerstatus is DIRECT.
     *
     * @return String : IP address where the request was forwarded
     */
    public final String getPeerhost() {
        return peerhost;
    }

    /**
     * Get content type.
     *
     * @return String : Content type
     */
    public final String getType() {
        return type;
    }

    @Override
    public final String toString() {
        return time + " " + url + " " + client;
    }

    @Override
    public final int hashCode() {
        int hash = 3;
        hash = 29 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 29 * hash + (this.client != null ? this.client.hashCode() : 0);
        hash = 29 * hash + (this.url != null ? this.url.hashCode() : 0);
        return hash;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Request other = (Request) obj;
        if (this.time != other.time) {
            return false;
        }
        if (!this.client.equals(other.client)) {
            return false;
        }
        if (!this.url.equals(other.url)) {
            return false;
        }
        return true;
    }
}
