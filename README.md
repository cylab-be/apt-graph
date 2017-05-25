# APT-GRAPH

[![Build Status](https://travis-ci.org/RUCD/apt-graph.svg?branch=master)](https://travis-ci.org/RUCD/apt-graph)
[![API](http://api123.io/api123-head.svg)](http://api123.io/api/apt-graph/head/index.html)
[![Coverage Status](https://coveralls.io/repos/github/RUCD/apt-graph/badge.svg?branch=master)](https://coveralls.io/github/RUCD/apt-graph?branch=master)

The focus of APT-GRAPH is the detection of Advanced Persistent Threat (APT). More specifically, the aim is to study proxy log files and to detect a domain used as Command and Control (C2) by an APT. The implemented algorithm models the traffic by means of a graph and tries to detect infections by looking for anomaly within the graph. The algorithm has been designed to work closelly with an analyst. This analyst can work interactively with a set of parameters and adapt the algorithm to focus on a specific type of APT.


## Requirements

- Java 8
- Apache Maven


## Maven modules

### Main modules

- **apt-graph**: is the parent module. It allows the build and the testing of all the modules at once.
- **core**: defines Objects and Classes used by the Batch Processor and the Server. It mainly defines the similarities used to build the graphs.
- **batch**: contains the Batch Processor. The Batch Processor is the module of the algorithm preprocessing the data .
- **server**: contains the JSON-RPC Server. The Server gives access to a set of parameters. With these parameters the Server is able to interactively complete the processing of the graph and gives the results to the UI (using JSON-RPC).
- **website**: contains the UI (HTML/JavaScript). This UI lets the analyst choose parameters needed by the Server and gives a visualisation of the results (computed graph and ranking list of suspicious domains).


### Auxiliary modules

- **integration**: checks that the Batch Processor and the Server work smootly together.
- **infection**: is an additional tool to simulate infections in a provided proxy log file.
- **traffic**: is an additional tool to study the traffic intensity of a provided proxy log file.
- **study**: is an additional tool allowing an in depth impact study of a given parameter on the detection. This tool uses Receiver Operating Characteristics (ROC) to evaluate the impact.
- **config**: is an additional tool which helps to generate configuration files used by the study tool.


## Quick Start

Get the latest version from GitHub.
```
git pull
```

Build all the modules together.
```
cd apt-graph
mvn clean install
```

Run the Batch Processor to build the graphs.
```
cd ../batch
./analyze.sh -i <proxy log file> -o <graphs directory>
```

There is a test file in _batch/src/test/resources/_. Use the following command to check the Batch Processor with the test file:
```
./analyze.sh -i ./src/test/resources/1000_http_requests.txt -o /tmp/mytest/
```

Run the server.
```
cd ../server
./start.sh -i <graphs directory>
```

By default, the UI is available at http://127.0.0.1:8000 and the JSON-RPC Server is at http://127.0.0.1:8080.

There is a folder in _server/src/test/resources_/ containing dummy graphs. Use the following command to check the Server with these graphs:
```
./start.sh -i ./src/test/resources/dummyDir/
```

Connect to the UI using a browser (http://127.0.0.1:8000). Choose your parameters as shown on the screenshots below and click on "Apply" to get the result.

If everything is alright you should get something like this:

![UI-example](./readme_fig/UI-example_1.png)

![UI-example](./readme_fig/UI-example_2.png)


## Algorithm

### Core

The Core defines the similarities used to compute the k-NN graphs of each user. The used similarities are the following:

* Time similarity:  
  $$
  \mu_{\Delta t} = \frac{1}{1+|\Delta t|}
  $$
  with $\Delta t$ defined as the temporal difference between the request timestamps (in second).

* Domain name based similarity:
  $$
  \mu_{dom} = \frac{\beta}{\beta_{tot}}
  $$
  with $\beta$ defined as the number of common labels between the two domain names, starting from the Top Level Domain (TLD), TLD excluded but equal to each other; $\beta_{tot}$ defined as the biggest number of labels between the two domain names, TLD excluded.


​	e.g.: _edition.cnn.com_ and _cnn.com_ have $\beta=1$, $\beta_{tot}=2$ and $\mu_{dom}=0.5

### Batch Processor

The Batch Processor is composed of the following processing steps:
1. parse a proxy log file (squid or JSON format);
2. split the data by user;
3. build k-NN graph of requests for each similarity and each user;
4. select the children requests among the neighbour requests (optional)
5. compute graph of domains for each similarity and each user;
6. store all necessary data in graphs directory (user graphs (_ip.address.ser_), list of users (_users.ser_), list of subnets (_subnets.ser_), k vallue (_k.ser_)).


### Server

The Server is composed of the following processing steps:
1. load the data of users selected by the analyst (_ip.address.ser_, _users.ser_, _subnets.ser_, _k.ser_);
2. merge similarity graphs for each user;
3. merge all user graphs;
4. prune the merged graph;
5. compute clusters in the graph (deprecated);
6. filter large clusters (deprecated);
7. clean the graph based on white listing (optional);
8. compute the rank list of suspicious domains.


### UI

The UI gives access to the following parameters:
* user or subnet;
* weights for fusion of the similarities;
* pruning threshold (absolute value or z-score);
* maximum cluster size (absolute value or z-score);
* white listing settings (optional): _on the go_ white listed domains, minimum number of requests by domain and by user;
* weights of ranking indexes.


## Usage

### batch

```
./analyze.sh -h
usage: java -jar batch-<version>.jar
-c <arg>   Select only temporal children (option, default: true)
-f <arg>   Specify format of input file (squid or json) (option,
			default: squid)
-h         Show this help
-i <arg>   Input log file (required)
-k <arg>   Impose k value of k-NN graphs (option, default: 20)
-o <arg>   Output directory for graphs (required)
-x <arg>   Overwrite existing graphs (option, default: false)
```


### server

```
./start.sh -h
usage: java -jar server-<version>.jar
-h             Show this help
-i <arg>       Input directory with graphs (required)
-study <arg>   Study output mode (false = web output, true = 
				study output) (option, default: false)
```


### infection

    ./infect.sh -h
    usage: java -jar infection-<version>.jar
     -d <arg>            APT domain name (required)
     -delay <arg>        Delay between start of the burst and injection
     					of APT (option for traffic APT, default: middle 
     					of the burst)
     -delta <arg>        Duration between two requests of the same burst 
     					(required for traffic APT)
     -distance <arg>     Minimal time distance between two injections (option
     					for traffic APT, default: no limitation)
     -duration <arg>     Minimal duration of a burst to allow APT injection
                         (required for traffic APT)
     -f <arg>            Specify format of input file (squid or json) (option,
                         default: squid)
     -h                  Show this help
     -i <arg>            Input log file (required)
     -injection <arg>    Maximal daily number of injections (option for traffic
     					APT, default: no limitation)
     -o <arg>            Output log file (required)
     -proportion <arg>   Injection rate in the possible bursts (1 = inject
     					in all possible bursts) (option for traffic APT,
     					default: 1)
     -step <arg>         Specify time step between periodic injections in 
     					milliseconds (required for periodic APT)
     -t <arg>            Type (periodic or traffic) (required)
     -u <arg>            Targeted user or subnet (required)


### traffic

```
./traffic.sh -h
usage: java -jar traffic-<version>.jar
 -f <arg>   Specify format of input file (squid or json) (option, 
 			default: squid)
 -h         Show this help
 -i <arg>   Input log file (required)
 -o <arg>   Output CSV file (required)
 -r <arg>   Time resolution in milliseconds (required)
```


### study

```
./study.sh -h
usage: java -jar study-<version>.jar
 -h         Show this help
 -i <arg>   Input configuration file (required)
 -x <arg>   Overwrite existing files (option, default: false)
```

### config

```
./config.sh -h
usage: java -jar config-<version>.jar
 -field <arg>   Configuration field to sweep (required)
 -h             Show this help
 -i <arg>       Input configuration file (default configuration line) 
 				(required)
 -multi <arg>   Sweep the given field as complement to stop value of 
 				the first field (option, default: no second field)
 -o <arg>       Output configuration file (required)
 -start <arg>   Start value of sweep (required)
 -step <arg>    Step of sweep (required)
 -stop <arg>    Stop value of sweep (required)
```

A typical default configuration line is: 

```
{"input_dir":"<input directory>",
"output_file":"<output file path>/ROC_anon.csv",
"n_apt_tot":"2","user":"108.142.213.0","feature_weights_time":"0.1",
"feature_weights_domain":"0.9","feature_weights_url":"0.0",
"feature_ordered_weights_1":"0.8","feature_ordered_weights_2":"0.2",
"prune_threshold":"0.00","max_cluster_size":"1000000",
"prune_z":"true","cluster_z":"false","whitelist":"true",
"white_ongo":"","number_requests":"5","ranking_weights_parents":"0.4",
"ranking_weights_children":"0.4","ranking_weights_requests":"0.2",
"apt_search":"true"}
```

## Futher details

Futher details can be found in the code itself, where each function has been documented.

## Licence

MIT License