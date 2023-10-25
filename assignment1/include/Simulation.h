#pragma once

#include <vector>
#include "Graph.h"
#include "Agent.h"

using std::string;
using std::vector;

class Simulation
{
public:
    Simulation(Graph g, vector<Agent> agents);

    void step();
    bool shouldTerminate() const;

    const Graph &getGraph() const;
    const vector<Agent> &getAgents() const;
    const Party &getParty(int partyId) const;
    const vector<vector<int>> getPartiesByCoalitions() const;
    vector<int> &getNumMandatByCoalition();
    vector<Agent> &getAgents();
    Party &getParty(int partyId);
    vector<vector<int>> &getPartiesByCoalition();

private:
    Graph mGraph;
    vector<Agent> mAgents;
    vector<int> mNumMandatByCoalition; 
    vector<vector<int>> mPartiesByCoalition;
};
