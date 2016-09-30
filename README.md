# APT-GRAPH

HTTP graph modeling for APT detection.

## Requirements
- java
- Apache Maven

## Starting the server

```
# get the latest version from github
git pull

# build the server
cd server
mvn clean package

# start the server
./start.sh

# by default, the server is available at http://127.0.0.1:8080
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

In the example below, 2 clusters are represented. The first cluster contains only one node (node 81: "Codeine..."). The second cluster has two nodes (node 108 and node 107). Node 108 has one edge to node 107 (similarity: 0.99) and node 107 has one edge to node 108.

```
[
    {
      "similarity": null,
      "k": 10,
      "nodes": [
        {
          "id": "81",
          "value": "Codeine (WILSON) 15mg x 30 $144.00 No Rx!!..."
        }
      ],
      "hashMap": {
        "(81 => Codeine (WILSON) 15mg x 30 $144.00 No Rx!!...": []
      }
    },
    {
      "similarity": null,
      "k": 10,
      "nodes": [
        {
          "id": "108",
          "value": "FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. vxd"
        },
        {
          "id": "107",
          "value": "FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. ubs"
        }
      ],
      "hashMap": {
        "(108 => FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. vxd)": [
          {
            "node": {
              "id": "107",
              "value": "FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. ubs"
            },
            "similarity": 0.9994660019874573
          }
        ],
        "(107 => FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. ubs)": [
          {
            "node": {
              "id": "108",
              "value": "FakeWatches, Buy Rep1icaWatch, iRolexOmega, Breitling, Bvlgari and other Genuine Swiss Rep1icaWatches. vxd"
            },
            "similarity": 0.9994660019874573
          }
        ]
      }
    },
    ...
```

![](./dummy-rpc.png)