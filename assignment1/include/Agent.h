#pragma once

#include <vector>
#include "Graph.h"

class SelectionPolicy;

class Agent
{
public:
    Agent(int agentId, int partyId, SelectionPolicy *selectionPolicy);
    //rule of 5
    Agent(const Agent& other);
    ~Agent();
    Agent& operator=(const Agent& other); 
    Agent(Agent&& other);
    Agent& operator=(Agent&& other); 

    int getPartyId() const;
    int getId() const;
    void step(Simulation &);
    Agent clone(int agentId, int partyId) const;

private:
    int mAgentId;
    int mPartyId;
    SelectionPolicy *mSelectionPolicy;
    bool mIdle;
};
