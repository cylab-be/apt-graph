# APT-GRAPH

HTTP graph modeling for APT detection.

## Requirements
- java
- Apache Maven

## Maven modules
- **core** : contains classes used by the batch processor and the server (Request, etc.).
- **batch** : contains the batch processor
- **server** : contains the json-rpc server and interactive fusion engine
- **apt-graph** : parent module, allows to build all modules at once...

## Usage

```
# get the latest version from github
git pull

# build all the modules together
cd apt-graph
mvn clean install

# to run the batch processor to build the graph
cd ../batch
./analyze.sh -i <proxy log file> -o <graph file>

# there is a test file in src/test/resources
# so you can test with
./analyze.sh -i ./src/test/resources/1000_http_requests.txt -o /tmp/mytest.ser

# to run the server
cd ../server
./start.sh -i <graph file>

# by default, the server is available at http://127.0.0.1:8080

# There is a dummy graph file in folder src/test/resources
# so you can make tests with
./start.sh -i ./src/test/resources/dummy_graph.ser
```

## Architecture

### Batch processor
- parses a proxy log file
- build one **requests k-nn graph** for each feature (time, url similarity, etc.)
- stores the graphs in a database

### UI
javascript UI that allows the user to choose:
- parameters for aggregating **request graphs** into **URL k-nn graphs**
- parameters for aggregating feature graphs into a single **combined graph** (simple version uses weighted average, a more evolved version might implement OWA or WOWA)
- prunning threshold for cutting low weight edges
- maximum cluster size for filtering resulting clusters

### Server
Exposes a json-rpc server that, based on user parameters, will:
- compute the **URL graphs**
- compute the **combined graph**
- **prune** the combined graph
- **cluster** the graph
- **filter** the clustered graph
- return the remaining nodes and edges

## Implemented RPC
### dummy

The **dummy** rpc returns a list of disconnected graphs (the clusters):
- Each graph consists of nodes and edges
- Each node has an id and a value
- the edges are represented as a hashmap that associates a source node and a destionation node

The example below contains one cluster of 7 nodes...

```
{
{
    "similarity": null,
    "k": 10,
    "nodes": [
        {
            "id": "1",
            "value": {
                "time": 1472083251,
                "elapsed": 920,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 765,
                "method": "GET",
                "url": "http://epnazrk.wmaj.ga/zlrsmtcc.html",
                "peerstatus": "DIRECT",
                "peerhost": "130.167.210.247",
                "type": "text/html"
            }
        },
        {
            "id": "2",
            "value": {
                "time": 1472083251,
                "elapsed": 444,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 755,
                "method": "GET",
                "url": "http://epnazrk.wmaj.ga/zjeglwir.html",
                "peerstatus": "DIRECT",
                "peerhost": "130.167.210.247",
                "type": "text/html"
            }
        },
        {
            "id": "3",
            "value": {
                "time": 1472083251,
                "elapsed": 590,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 1083,
                "method": "GET",
                "url": "http://kfiger.wfltjx.cc/uxmt.html",
                "peerstatus": "DIRECT",
                "peerhost": "47.238.242.2",
                "type": "text/html"
            }
        },
        {
            "id": "4",
            "value": {
                "time": 1472083251,
                "elapsed": 683,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 1419,
                "method": "GET",
                "url": "http://isogbg.hgwpxah.nz/roeefw.html",
                "peerstatus": "DIRECT",
                "peerhost": "233.4.82.7",
                "type": "text/html"
            }
        },
        {
            "id": "5",
            "value": {
                "time": 1472083251,
                "elapsed": 442,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 1960,
                "method": "GET",
                "url": "http://rkfko.apyeqwrqg.cm/rdhufye.html",
                "peerstatus": "DIRECT",
                "peerhost": "249.70.126.8",
                "type": "text/html"
            }
        },
        {
            "id": "6",
            "value": {
                "time": 1472083251,
                "elapsed": 276,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 111,
                "method": "GET",
                "url": "http://ootlgeqo.fomu.ve/sfidbhq.html",
                "peerstatus": "DIRECT",
                "peerhost": "243.179.195.173",
                "type": "text/html"
            }
        },
        {
            "id": "0",
            "value": {
                "time": 1472083251,
                "elapsed": 575,
                "client": "198.36.158.8",
                "code": "TCP_MISS",
                "status": 200,
                "bytes": 1411,
                "method": "GET",
                "url": "http://ajdd.rygxzzaid.mk/xucjehmkd.html",
                "peerstatus": "DIRECT",
                "peerhost": "118.220.140.185",
                "type": "text/html"
            }
        }
    ],
    "hashMap": {
        "(1 => 1472083251\thttp://epnazrk.wmaj.ga/zlrsmtcc.html 198.36.158.8)": [
            {
                "node": {
                    "id": "4",
                    "value": {
                        "time": 1472083251,
                        "elapsed": 683,
                        "client": "198.36.158.8",
                        "code": "TCP_MISS",
                        "status": 200,
                        "bytes": 1419,
                        "method": "GET",
                        "url": "http://isogbg.hgwpxah.nz/roeefw.html",
                        "peerstatus": "DIRECT",
                        "peerhost": "233.4.82.7",
                        "type": "text/html"
                    }
                },
                "similarity": 1
            },
            {
                "node": {
                    "id": "2",
                    "value": {
                        "time": 1472083251,
                        "elapsed": 444,
                        "client": "198.36.158.8",
                        "code": "TCP_MISS",
                        "status": 200,
                        "bytes": 755,
                        "method": "GET",
                        "url": "http://epnazrk.wmaj.ga/zjeglwir.html",
                        "peerstatus": "DIRECT",
                        "peerhost": "130.167.210.247",
                        "type": "text/html"
                    }
                },
                "similarity": 1
            },
            {
                "node": {
                    "id": "5",
                    "value": {
                        "time": 1472083251,
                        "elapsed": 442,
                        "client": "198.36.158.8",
                        "code": "TCP_MISS",
                        "status": 200,
                        "bytes": 1960,
                        "method": "GET",
                        "url": "http://rkfko.apyeqwrqg.cm/rdhufye.html",
                        "peerstatus": "DIRECT",
                        "peerhost": "249.70.126.8",
                        "type": "text/html"
                    }
                },
                "similarity": 1
            },
            {
                "node": {
                    "id": "6",
            ...
```

 ![dummy-rpc](dummy-rpc.png)
