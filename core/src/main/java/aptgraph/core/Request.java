package aptgraph.core;

import java.io.Serializable;

/**
 *
 * @author Thibault Debatty
 */
public class Request implements Serializable {
    public int time; //Unix Timestamp as UTC seconds with a millisecond resolution
    public int elapsed; //In millisecond
    public String client;
    public String code;
    public int status; //HTTP Status Code
    public int bytes;
    public String method;
    public String url;
    public String peerstatus;
    public String peerhost; //This is the origin IP of the server if peerstatus is DIRECT
    public String type;

    @Override
    public String toString() {
        return time + "\t" + url + " " + client;
    }

}
