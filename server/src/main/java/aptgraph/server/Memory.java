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

import aptgraph.core.Domain;
import info.debatty.java.graphs.Graph;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 *
 * @author Thomas Gilon
 */
public class Memory {
    // List of all the users
    private ArrayList<String> all_users_list;
    // List of all the subnets
    private ArrayList<String> all_subnets_list;
    // Current user (could be a subnet)
    private String user = "";
    // Current list of users
    private ArrayList<String> users_list;
    // Value of the k of manipulated graphs
    private int k;
    // List of domains in the graphs
    // term 'byUsers' will contain all domains without merge of requests
    // (key = user:domain)
    // term 'all' will contain all merged domains
    // (key = domain)
    private HashMap<String, HashMap<String, Domain>> all_domains
            = new HashMap<String, HashMap<String, Domain>>();
    // List of individuals graphs of feature for each user
    private HashMap<String, LinkedList<Graph<Domain>>> users_graphs
            = new HashMap<String, LinkedList<Graph<Domain>>>();
    // Feature weights
    private double[] feature_weights;
    // Feature ordered weights
    private double[] feature_ordered_weights;
    // Graph, after fusion of users
    private Graph<Domain> merged_graph;
    // Mean and Variance of similarities of merged graph
    private double[] mean_var_similarities;
    // Histogram data of similarities of merged graph
    private HistData hist_similarities;
    // Pruning threshold given by user (before conversion, if needed)
    private double prune_threshold_temp;
    // Pruning threshold used
    private double prune_threshold;
    // Indicator for prune_threshold_temp : z value or not
    private boolean prune_z_bool;
    // Mean and Variance of the cluster sizes
    private double[] mean_var_clusters;
    // Histogram data of cluster sizes
    private HistData hist_cluster;
    // Clusters
    private ArrayList<Graph<Domain>> clusters;
    // Max cluster size given by user (before conversion, if needed)
    private double max_cluster_size_temp;
    // Max cluster size used
    private double max_cluster_size;
    // Indicator for max_cluster_size_temp : z value or not
    private boolean cluster_z_bool;
    // List of graphs after filtering
    private LinkedList<Graph<Domain>> filtered;
    // Indicator for white listing
    private boolean whitelist_bool;
    // Path of the white list
    private static final Path PATH = Paths.get("src/main/resources/hosts");
    // List of white listed domains on the go
    private String white_ongo;
    // Minimum number of requests sent by user for a given domain
    private double number_requests;
    // List of effectivly whitelisted domains
    private LinkedList<Domain> whitelisted;
    // List of graphs after filtering and white listing
    private LinkedList<Graph<Domain>> filtered_white_listed;
    // Ranking weights
    private double[] ranking_weights;
    // Indicator for search of APT.FINDME.be
    private boolean apt_search;
    // Info of Ranking for print
    private String ranking_print;
    // Info of Ranking
    private TreeMap<Double, LinkedList<Domain>> ranking
            = new TreeMap<Double, LinkedList<Domain>>();
    // Standard output on UI
    private String stdout;

    /**
     * Get the list of all the users.
     * @return all_users_list
     */
    public final ArrayList<String> getAllUsersList() {
        return this.all_users_list;
    }

    /**
     * Set the list of all the users.
     * @param all_users_list
     */
    public final void setAllUsersList(
            final ArrayList<String> all_users_list) {
        this.all_users_list = all_users_list;
    }

    /**
     * Get the list of all the subnets.
     * @return all_subnets_list
     */
    public final ArrayList<String> getAllSubnetsList() {
        return this.all_subnets_list;
    }

    /**
     * Set the list of all the subnet.
     * @param all_subnets_list
     */
    public final void setAllSubnetsList(
            final ArrayList<String> all_subnets_list) {
        this.all_subnets_list = all_subnets_list;
    }

    /**
     * Get the current user.
     * @return user
     */
    public final String getUser() {
        return this.user;
    }

    /**
     * Set the current user.
     * @param user
     */
    public final void setUser(final String user) {
        this.user = user;
    }

    /**
     * Get the current list of users.
     * @return users_list
     */
    public final ArrayList<String> getUsersList() {
        return this.users_list;
    }

    /**
     * Set the current list of users.
     * @param users_list
     */
    public final void setUsersList(final ArrayList<String> users_list) {
        this.users_list = users_list;
    }

    /**
     * Get the current k.
     * @return k
     */
    public final int getCurrentK() {
        return this.k;
    }

    /**
     * Set the current k.
     * @param k
     */
    public final void setCurrentK(final int k) {
        this.k = k;
    }

    /**
     * Get the list of domains.
     * @return all_domains
     */
    public final HashMap<String, HashMap<String, Domain>> getAllDomains() {
        if (this.all_domains.get("byUsers") == null) {
            this.all_domains.put("byUsers", new HashMap<String, Domain>());
        }
        if (this.all_domains.get("all") == null) {
            this.all_domains.put("all", new HashMap<String, Domain>());
        }
        return this.all_domains;
    }

    /**
     * Get the list of domains for specific mode.
     * @param mode
     * @return all_domains
     */
    public final HashMap<String, Domain> getAllDomains(final String mode) {
        if (this.all_domains.get(mode) == null) {
            this.all_domains.put(mode, new HashMap<String, Domain>());
        }
        return this.all_domains.get(mode);
    }

    /**
     * Set the list of domains.
     * @param all_domains_by_users
     * @param all_domains_all
     */
    public final void setAllDomains(
            final HashMap<String, Domain> all_domains_by_users,
            final HashMap<String, Domain> all_domains_all) {
        this.all_domains.put("byUsers", all_domains_by_users);
        this.all_domains.put("all", all_domains_all);
    }

    /**
     * Set the list of domains (only for domains by users).
     * @param all_domains_by_users
     */
    public final void setAllDomainsByUsers(
            final HashMap<String, Domain> all_domains_by_users) {
        this.all_domains.put("byUsers", all_domains_by_users);
    }

    /**
     * Set the list of domains (only for global domains).
     * @param all_domains_all
     */
    public final void setAllDomainsAll(
            final HashMap<String, Domain> all_domains_all) {
        this.all_domains.put("all", all_domains_all);
    }

    /**
     * Get the list of graphs of feature for each user.
     * @return users_graphs
     */
    public final HashMap<String, LinkedList<Graph<Domain>>> getUsersGraphs() {
        return this.users_graphs;
    }

    /**
     * Set the list of graphs of feature for each user.
     * @param users_graphs
     */
    public final void setUsersGraphs(
            final HashMap<String, LinkedList<Graph<Domain>>> users_graphs) {
        this.users_graphs = users_graphs;
    }

    /**
     * Get the feature weights.
     * @return feature_weights
     */
    public final double[] getFeatureWeights() {
        return this.feature_weights.clone();
    }

    /**
     * Set the feature weights.
     * @param feature_weights
     */
    public final void setFeatureWeights(final double[] feature_weights) {
        this.feature_weights = feature_weights.clone();
    }

    /**
     * Get the feature ordered weights.
     * @return feature_ordered_weights
     */
    public final double[] getFeatureOrderedWeights() {
        return this.feature_ordered_weights.clone();
    }

    /**
     * Set the feature ordered weights.
     * @param feature_ordered_weights
     */
    public final void setFeatureOrderedWeights(
            final double[] feature_ordered_weights) {
        this.feature_ordered_weights = feature_ordered_weights.clone();
    }

    /**
     * Get the Merged Graph.
     * @return merged_graph
     */
    public final Graph<Domain> getMergedGraph() {
        return this.merged_graph;
    }

    /**
     * Set the Merged Graph.
     * @param merged_graph
     */
    public final void setMergedGraph(final Graph<Domain> merged_graph) {
        this.merged_graph = merged_graph;
    }

    /**
     * Get the mean and variance for the similarities of merged graph.
     * @return mean_var_similarities
     */
    public final double[] getMeanVarSimilarities() {
        return this.mean_var_similarities.clone();
    }

    /**
     * Set the mean and variance for the similarities of merged graph.
     * @param mean_var_similarities
     */
    public final void setMeanVarSimilarities(
            final double[] mean_var_similarities) {
        this.mean_var_similarities = mean_var_similarities.clone();
    }

    /**
     * Get the histogram data of similarities of merged graph.
     * @return hist_similarities_store
     */
    public final HistData getHistDataSimilarities() {
        return this.hist_similarities;
    }

    /**
     * Set the histogram data of similarities of merged graph.
     * @param hist_similarities
     */
    public final void setHistDataSimilarities(
            final HistData hist_similarities) {
        this.hist_similarities = hist_similarities;
    }

    /**
     * Get the pruning threshold given by user (before conversion, if needed).
     * @return prune_threshold_temp
     */
    public final double getPruningThresholdTemp() {
        return this.prune_threshold_temp;
    }

    /**
     * Set the pruning threshold given by user (before conversion, if needed).
     * @param prune_threshold_temp
     */
    public final void setPruningThresholdTemp(
            final double prune_threshold_temp) {
        this.prune_threshold_temp = prune_threshold_temp;
    }

    /**
     * Get the pruning threshold used.
     * @return prune_threshold
     */
    public final double getPruningThreshold() {
        return this.prune_threshold;
    }

    /**
     * Set the pruning threshold used.
     * @param prune_threshold
     */
    public final void setPruningThreshold(
            final double prune_threshold) {
        this.prune_threshold = prune_threshold;
    }

    /**
     * Get the indicator for prune_threshold_temp : z value or not.
     * @return prune_z_bool
     */
    public final boolean getPruneZBool() {
        return this.prune_z_bool;
    }

    /**
     * Set the indicator for prune_threshold_temp : z value or not.
     * @param prune_z_bool
     */
    public final void setPruneZBool(
            final boolean prune_z_bool) {
        this.prune_z_bool = prune_z_bool;
    }

    /**
     * Get the mean and Variance of the cluster sizes.
     * @return mean_var_cluster
     */
    public final double[] getMeanVarClusters() {
        return this.mean_var_clusters.clone();
    }

    /**
     * Set the mean and Variance of the cluster sizes.
     * @param mean_var_clusters
     */
    public final void setMeanVarClusters(
            final double[] mean_var_clusters) {
        this.mean_var_clusters = mean_var_clusters.clone();
    }

    /**
     * Get the histogram data of cluster sizes.
     * @return hist_cluster
     */
    public final HistData getHistDataClusters() {
        return this.hist_cluster;
    }

    /**
     * Set the histogram data of cluster sizes.
     * @param hist_cluster
     */
    public final void setHistDataClusters(
            final HistData hist_cluster) {
        this.hist_cluster = hist_cluster;
    }

    /**
     * Get the clusters.
     * @return clusters
     */
    public final ArrayList<Graph<Domain>> getClusters() {
        return this.clusters;
    }

    /**
     * Set the clusters.
     * @param clusters
     */
    public final void setClusters(
            final ArrayList<Graph<Domain>> clusters) {
        this.clusters = clusters;
    }

    /**
     * Get the max cluster size given by user (before conversion, if needed).
     * @return max_cluster_size_temp
     */
    public final double getMaxClusterSizeTemp() {
        return this.max_cluster_size_temp;
    }

    /**
     * Set the max cluster size given by user (before conversion, if needed).
     * @param max_cluster_size_temp
     */
    public final void setMaxClusterSizeTemp(
            final double max_cluster_size_temp) {
        this.max_cluster_size_temp = max_cluster_size_temp;
    }

    /**
     * Get the max cluster size used.
     * @return max_cluster_size
     */
    public final double getMaxClusterSize() {
        return this.max_cluster_size;
    }

    /**
     * Set the max cluster size used.
     * @param max_cluster_size
     */
    public final void setMaxClusterSize(
            final double max_cluster_size) {
        this.max_cluster_size = max_cluster_size;
    }

    /**
     * Get the indicator for max_cluster_size_temp : z value or not.
     * @return cluster_z_bool
     */
    public final boolean getClusterZBool() {
        return this.cluster_z_bool;
    }

    /**
     * Set the indicator for max_cluster_size_temp : z value or not.
     * @param cluster_z_bool
     */
    public final void setClusterZBool(
            final boolean cluster_z_bool) {
        this.cluster_z_bool = cluster_z_bool;
    }

    /**
     * Get the list of graphs after filtering.
     * @return filtered
     */
    public final LinkedList<Graph<Domain>> getFiltered() {
        return this.filtered;
    }

    /**
     * Set the list of graphs after filtering.
     * @param filtered
     */
    public final void setFiltered(
            final LinkedList<Graph<Domain>> filtered) {
        this.filtered = filtered;
    }

    /**
     * Get the indicator for white listing.
     * @return whitelist_bool
     */
    public final boolean getWhitelistBool() {
        return this.whitelist_bool;
    }

    /**
     * Set the indicator for white listing.
     * @param whitelist_bool
     */
    public final void setWhitelistBool(
            final boolean whitelist_bool) {
        this.whitelist_bool = whitelist_bool;
    }

    /**
     * Get the path of the white list.
     * @return PATH
     */
    public final Path getWhiteListPath() {
        return Memory.PATH;
    }

    /**
     * Get the list of white listed domains on the go.
     * @return white_ongo
     */
    public final String getWhiteOngo() {
        return this.white_ongo;
    }

    /**
     * Set the list of effectivly whitelisted domains.
     * @param white_ongo
     */
    public final void setWhiteOngo(
            final String white_ongo) {
        this.white_ongo = white_ongo;
    }

    /**
     * Get the minimum number of requests sent by user for a given domain.
     * @return number_requests
     */
    public final double getNumberRequests() {
        return this.number_requests;
    }

    /**
     * Set the minimum number of requests sent by user for a given domain.
     * @param number_requests
     */
    public final void setNumberRequests(
            final double number_requests) {
        this.number_requests = number_requests;
    }

    /**
     * Get the list of effectivly whitelisted domains.
     * @return whitelisted
     */
    public final LinkedList<Domain> getWhitelisted() {
        return this.whitelisted;
    }

    /**
     * Set the list of white listed domains on the go.
     * @param whitelisted
     */
    public final void setWhitelisted(
            final LinkedList<Domain> whitelisted) {
        this.whitelisted = whitelisted;
    }

    /**
     * Get the list of graphs after filtering and white listing.
     * @return filtered_white_listed
     */
    public final LinkedList<Graph<Domain>> getFilteredWhiteListed() {
        return this.filtered_white_listed;
    }

    /**
     * Set the list of graphs after filtering and white listing.
     * @param filtered_white_listed
     */
    public final void setFilteredWhiteListed(
            final LinkedList<Graph<Domain>> filtered_white_listed) {
        this.filtered_white_listed = filtered_white_listed;
    }

    /**
     * Get the ranking weights.
     * @return ranking_weights
     */
    public final double[] getRankingWeights() {
        return this.ranking_weights.clone();
    }

    /**
     * Set the ranking weights.
     * @param ranking_weights
     */
    public final void setRankingWeights(
            final double[] ranking_weights) {
        this.ranking_weights = ranking_weights.clone();
    }

    /**
     * Get the indicator for search of APT.FINDME.be.
     * @return apt_search
     */
    public final boolean getAptSearch() {
        return this.apt_search;
    }

    /**
     * Set the indicator for search of APT.FINDME.be.
     * @param apt_search
     */
    public final void setAptSearch(
            final boolean apt_search) {
        this.apt_search = apt_search;
    }

    /**
     * Get the info of Ranking for print.
     * @return ranking_print
     */
    public final String getRankingPrint() {
        return this.ranking_print;
    }

    /**
     * Set the info of Ranking for print.
     * @param ranking_print
     */
    public final void setRankingPrint(
            final String ranking_print) {
        this.ranking_print = ranking_print;
    }

    /**
     * Concatenate input to the info of Ranking for print.
     * @param ranking_print
     */
    public final void concatRankingPrint(
            final String ranking_print) {
        this.ranking_print = this.ranking_print.concat(ranking_print);
    }

    /**
     * Get the info of Ranking.
     * @return ranking
     */
    public final TreeMap<Double, LinkedList<Domain>> getRanking() {
        return this.ranking;
    }

    /**
     * Set the info of Ranking.
     * @param ranking
     */
    public final void setRanking(
            final TreeMap<Double, LinkedList<Domain>> ranking) {
        this.ranking = ranking;
    }

    /**
     * Get the standard output on UI.
     * @return stdout
     */
    public final String getStdout() {
        return this.stdout;
    }

    /**
     * Set the standard output on UI.
     * @param stdout
     */
    public final void setStdout(
            final String stdout) {
        this.stdout = stdout;
    }

    /**
     * Concatenate input to the standard output on UI.
     * @param input
     */
    public final void concatStdout(
            final String input) {
        this.stdout = this.stdout.concat(input);
    }
}
