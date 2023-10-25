#include "Simulation.h"

Simulation::Simulation(Graph graph, vector<Agent> agents) : mGraph(graph), mAgents(agents), mNumMandatByCoalition(), mPartiesByCoalition()
{
    // update coalitionId and state for each party that belong an agent
    // update partyId and num of mandates for each coalition
    mPartiesByCoalition.resize(mAgents.size());
    for (unsigned int i=0; i<mAgents.size(); i++){
        int partyID = mAgents[i].getPartyId();
        Party &coalitionParty = mGraph.getParty(partyID);
        coalitionParty.setCoalitionId(i);
        coalitionParty.setState(Joined);
        mPartiesByCoalition[i].push_back(partyID);
        mNumMandatByCoalition.push_back(coalitionParty.getMandates());
    }
}

void Simulation::step()
{
    // party's step
    for (int i=0; i<mGraph.getNumVertices(); i++)
        mGraph.getParty(i).step(*this);
    // agent's step
    for (unsigned int i=0; i<mAgents.size(); i++){
        mAgents[i].step(*this);
    }
}

// return true if there is coalition with more than 60 mandates or all parties already joined
bool Simulation::shouldTerminate() const
{
    int sumMandats = 0;
    for (unsigned int i=0; i<mNumMandatByCoalition.size(); i++){
        if (mNumMandatByCoalition[i] >= 61) // succeed create coalition
            return true;
        sumMandats = sumMandats+mNumMandatByCoalition[i];
    }
    if (sumMandats == 120) // all parties already joined
        return true;
    return false;
}

const Graph &Simulation::getGraph() const
{
    return mGraph;
}

const vector<Agent> &Simulation::getAgents() const
{
    return mAgents;
}

const Party &Simulation::getParty(int partyId) const
{
    return mGraph.getParty(partyId);
}

/// This method returns a "coalition" vector, where each element is a vector of party IDs in the coalition.
/// At the simulation initialization - the result will be [[agent0.partyId], [agent1.partyId], ...]
const vector<vector<int>> Simulation::getPartiesByCoalitions() const
{
    return mPartiesByCoalition;
}

vector<int> &Simulation::getNumMandatByCoalition()
{
    return mNumMandatByCoalition;
}

vector<Agent> &Simulation::getAgents()
{
    return mAgents;
}

Party &Simulation::getParty(int partyId)
{
    return mGraph.getParty(partyId);
}

vector<vector<int>> &Simulation::getPartiesByCoalition()
{
    return mPartiesByCoalition;
}