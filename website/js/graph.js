/* eslint-env jquery */
function drawGraph(jsonData, jsonPost, printRequests, anonymize) {
  var data = jsonData;
  var links = []; // dict = {source: , target: , value: ,}
  var similarity;
  var target;
  for (var n = 0; n < data.length; n++) {
    var clusterNodes = data[n].nodes;
    var neighbors = data[n].neighbors;
    for (var i = 0; i < clusterNodes.length; i++) {
      var currentNode = clusterNodes[i];
      var name = currentNode.name;
      var nodeNeighbors = neighbors[name];
      if (nodeNeighbors.length === 0) {
        similarity = 0;
        links.push({'source': name, 'target': name, 'value': similarity});
      } else {
        for (var m = 0; m < nodeNeighbors.length; m++) {
          var neighbor = nodeNeighbors[m];
          var source = name;
          for (var t = 0; t < clusterNodes.length; t++) {
            if (clusterNodes[t].name === neighbor.node) {
              target = clusterNodes[t].name;
              similarity = neighbor.similarity;
              break;
            }
          }
          links.push({'source': source, 'target': target, 'value': similarity});
        }
      }

    }
  }
  //  console.log(links);
  var nodes = {};

  // Compute the distinct nodes from the links.
  links.forEach(function(link) {
    link.source = nodes[link.source] ||
      (nodes[link.source] = {name: link.source});
    link.target = nodes[link.target] ||
      (nodes[link.target] = {name: link.target});
    link.value = +link.value;
  });

  //  console.log(nodes);

  var width = window.innerWidth;
  var  height = window.innerHeight;
  var heightPanels = 150;
  var sideBarHeight = document.getElementById('side_bar').clientHeight;
  var graphWidth = document.getElementById('graph').clientWidth;

  var force = d3.layout.force()
    .nodes(d3.values(nodes))
    .links(links)
    .size([width / 2 , height / 1.2])
    .linkDistance(100)
    .charge(-300)
    .on('tick', tick)
    .start();

  // remove if anything was already drawn on the screen
  d3.select('body').select('#container').select('#parent').select('#graph')
  .select('svg').remove();
  // draw new graph

  var svg = d3.select('body').select('#container').select('#parent')
  .select('#graph').append('svg')
    .attr('width', graphWidth)
    .attr('height', sideBarHeight);

  // build the arrow.
  svg.append('svg:defs').selectAll('marker')
    .data(['end'])      // Different link/path types can be defined here
  .enter().append('svg:marker')    // This section adds in the arrows
    .attr('id', String)
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 15)
    .attr('refY', -1.5)
    .attr('fill', 'darkgrey')
    .attr('markerWidth', 6)
    .attr('markerHeight', 6)
    .attr('orient', 'auto')
  .append('svg:path')
    .attr('d', 'M0,-5L10,0L0,5');
  /*eslint-disable no-unused-vars*/
  // add the links and the arrows
  var pathIndex = 0;
  var path = svg.append('svg:g').selectAll('path')
    .data(force.links())
  .enter().append('svg:path')
    .attr('class', function(d) { return 'link ' + d.type; })
    .attr('class', 'link')
    .attr('id', function() {
      pathIndex = pathIndex + 1;
      return pathIndex;
    })
    .on('mouseover', function(d) {
          var g = d3.select(this); // The node
          var pathInfo = d;
          var tooltip = svg.append('g')
                              .attr('id', 'tooltip');
          tooltip.append('rect')
              .attr('x', Math.abs((pathInfo.source.x + pathInfo.target.x) / 2))
              .attr('y', Math.abs((pathInfo.source.y + pathInfo.target.y) / 2))
              .attr('height', 40)
              .attr('width', 80)
              .attr('stroke-width', 2)
              .attr('stroke', 'black')
              .style('fill', 'white');
          tooltip.append('text')
              .attr('x', Math.abs((pathInfo.source.x + pathInfo.target.x) /
                2) + 25)
              .attr('y', Math.abs((pathInfo.source.y + pathInfo.target.y) /
                2) + 25)
              .text(function() {return Math.round(pathInfo.value * 100) /
                100.0; });
        })
      .on('mouseout', function() {
        // Remove the tooltip text on mouse out.
        svg.selectAll('#tooltip').remove();
      })
      .attr('marker-end', function(d) {
        if (d.value === 0) {
          return '';
        } else {
          return 'url(#end)';
        }});

  // define the nodes
  var node = svg.selectAll('.node')
    .data(force.nodes())
  .enter().append('g')
    .attr('class', 'node')
    .on('click', function(d) {
      var g = d3.select(this); // The node
      // The class is used to remove the additional text later
      if (d3.select(this).select('text.info')[0][0] === null) {
        var jsonbody = {'jsonrpc': '2.0',
                'method': 'getRequests',
                'params': [d.name],
                'id': 167
              };
        var request = jsonPost('http://127.0.0.1:8080', jsonbody);
        request.addEventListener('load', function() {
          if (request.readyState == 4 && request.status == 200) {
            var jsonRequests = JSON.parse(request.responseText);
            printRequests(jsonRequests.result);
            g.select('text').remove();
            var info = g.append('text')
              .classed('info', true)
              .attr('x', 12)
              .attr('dy', '.35em')
              .attr('font-size', '30px')
              .text(function(d) { return anonymize(document.getElementById(
                'anonqbutton').checked, d.name) + ' (' + jsonRequests.result
                .length + ')'; });
            $(document).ready(function() {
              $('#panel_head').text('Request Status = ' + request.status + ' ' +
                request.statusText);
              $('#panel_head').css('color', 'green');
              $('#panel_body').text('Server Response: Requests of ' +
                anonymize(document.getElementById('anon_button').checked,
                  d.name) + ' loaded');
            });
          } else if (request.status != 400) {
            $(document).ready(function() {
                $('#panel_head').text('Request Status Error = ' +
                  request.status + ' ' + request.statusText);
                $('#panel_head').css('color', 'red');
                $('#panel_body').text('Server Response: Error while loading' +
                  ' requests of ' + anonymize(document.getElementById(
                  'anon_button').checked, d.name));
              });
          }});
      } else {
        d3.select(this).select('text.info').remove();
        d3.select(this).append('text')
          .attr('x', 12)
          .attr('dy', '.35em')
          .attr('font-size', '10xp')
          .text(function(d) { return anonymize(document.getElementById(
            'anon_button').checked, d.name); });
      }
    })
  /*    .on('mouseout', function() {
      // Remove the info text on mouse out.
      d3.select(this).select('text.info').remove();
    })*/
    .call(force.drag);

  // add the nodes
  node.append('circle')
    .attr('r', function(d) {
      return 5;});
  /*eslint-enable no-unused-vars*/
  // add the text
  node.append('text')
    .attr('x', 12)
    .attr('dy', '.35em')
    .attr('font-size', '10xp')
    .text(function(d) {
      return anonymize(document.getElementById('anon_button').checked, d.name);
    });

  resize();
  d3.select(window).on('resize', resize);
  // add the curvy lines
  function tick() {
    path.attr('d', function(d) {
        var dx = d.target.x - d.source.x;
        var dy = d.target.y - d.source.y;
        var dr = Math.sqrt(dx * dx + dy * dy);
        return 'M' +
          d.source.x + ',' +
          d.source.y + 'A' +
          dr + ',' + dr + ' 0 0,1 ' +
          d.target.x + ',' +
          d.target.y;
      });

    node
      .attr('transform', function(d) {
      return 'translate(' + d.x + ',' + d.y + ')'; });
  }

  function resize() {
    var graphWidth = document.getElementById('graph').clientWidth;
    svg.attr('width', graphWidth).attr('height', height - heightPanels);
    force.size([width / 2, height / 1.2]).resume();
  }
}

// Returns a list of all nodes under the root.
function flatten(root) {
  var nodes = [];
  var i = 0;

  function recurse(node) {
    if (node.children) { node.children.forEach(recurse); }
    if (!node.id) { node.id = ++i; }
    nodes.push(node);
  }

  recurse(root);
  return nodes;
}
