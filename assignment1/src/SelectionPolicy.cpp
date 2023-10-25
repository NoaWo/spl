#include "SelectionPolicy.h"
#include "Graph.h"

// return partyId of the chosen party (select by most mandates) or -1 if there is no party
int MandatesSelectionPolicy::select (const Graph& graph, int partyID) const
{
    int leadParty = -1;
    for (int i=0; i<graph.getNumVertices(); i++) {
        int weight = graph.getEdgeWeight(partyID, i);
        // if there is edge and if the mandates number is bigger
        if (weight > 0 && ((leadParty == -1 || graph.getParty(i).getMandates() > graph.getParty(leadParty).getMandates()))) {
            State state = graph.getParty(i).getState();
            if (state != Joined) { // if state available
                bool suggest = true;
                if (state == CollectingOffers) { // need to check previos offers
                    int coalitionId = graph.getParty(partyID).getCoalition();
                    const vector<int>& offers = graph.getParty(i).getOffers();
                    for (unsigned int j=0; suggest && j<offers.size(); j++) {
                        if (offers[j] == coalitionId) // already have offer from this coalition
                            suggest = false;
                    }
                }
                if (suggest)
                    leadParty = i;            
            }
        }
    }
    return leadParty;
}


// return partyId of the chosen party (select by highest edge weight) or -1 if there is no party
int EdgeWeightSelectionPolicy::select (const Graph& graph, int partyID) const
{
    int leadParty = -1;
    for (int i=0; i<graph.getNumVertices(); i++) {
        int weight = graph.getEdgeWeight(partyID, i);
        // if there is edge and if the edge weight is higher
        if (weight > 0 && (leadParty == -1 || weight > graph.getEdgeWeight(partyID, leadParty))) {
            State state = graph.getParty(i).getState();
            if (state != Joined) { // if state available
                bool suggest = true;
                if (state == CollectingOffers) { // need to check previos offers
                    int coalitionId = graph.getParty(partyID).getCoalition();
                    const vector<int>& offers = graph.getParty(i).getOffers();
                    for (unsigned int j=0; suggest && j<offers.size(); j++) {
                        if (offers[j] == coalitionId) // already have offer from this coalition
                            suggest = false;
                    }
                }
                if (suggest)
                    leadParty = i;
            }
        }
    }
    return leadParty;    
}

// return new inastance of MandatesSelectionPolicy
SelectionPolicy* MandatesSelectionPolicy::clone() const
{
    return new MandatesSelectionPolicy();
}

// return new inastance of EdgeWeightSelectionPolicy
SelectionPolicy* EdgeWeightSelectionPolicy::clone() const
{
    return new EdgeWeightSelectionPolicy();
}