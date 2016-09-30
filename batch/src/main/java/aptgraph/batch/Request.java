/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package aptgraph.batch;

/**
 *
 * @author Thibault Debatty
 */
class Request {
    int time;
    String client;
    String url;

    @Override
    public String toString() {
        return time + "\t" + url + " " + client;
    }

}
