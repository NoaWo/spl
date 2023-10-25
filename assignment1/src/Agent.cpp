#include "Agent.h"
#include "SelectionPolicy.h"
#include "Simulation.h"

Agent::Agent(int agentId, int partyId, SelectionPolicy *selectionPolicy) : mAgentId(agentId), mPartyId(partyId), mSelectionPolicy(selectionPolicy), mIdle(false)
{   
}

// Copy Constructor
Agent::Agent(const Agent& other): mAgentId(other.mAgentId), mPartyId(other.mPartyId), mSelectionPolicy((*other.mSelectionPolicy).clone()), mIdle(other.mIdle)
{
}

// Destructor
Agent::~Agent()
{
    if (mSelectionPolicy != nullptr) {
        delete(mSelectionPolicy);
        mSelectionPolicy = nullptr;
    }
}

// Copy Assignment Operator
Agent& Agent::operator=(const Agent& other)
{
    if (this != &other) {
        if (mSelectionPolicy != nullptr) {
            delete(mSelectionPolicy);
            mSelectionPolicy = nullptr;
        }
        mAgentId = other.mAgentId;
        mPartyId = other.mPartyId;
        mSelectionPolicy = (*other.mSelectionPolicy).clone();
        mIdle = other.mIdle;
    }
    return *this;
}

// Move Constructor
Agent::Agent(Agent&& other): mAgentId(other.mAgentId), mPartyId(other.mPartyId), mSelectionPolicy(other.mSelectionPolicy), mIdle(other.mIdle)
{
    other.mSelectionPolicy = nullptr;
}

// Move Assignment Operator
Agent& Agent::operator=(Agent&& other)
{
     if (this != &other) {
        if (mSelectionPolicy != nullptr) {
            delete(mSelectionPolicy);
            mSelectionPolicy = nullptr;
        }
        mAgentId = other.mAgentId;
        mPartyId = other.mPartyId;
        mSelectionPolicy = other.mSelectionPolicy;
        mIdle = other.mIdle;
        other.mSelectionPolicy = nullptr;
    }
    return *this;
}

int Agent::getId() const
{
    return mAgentId;
}

int Agent::getPartyId() const
{
    return mPartyId;
}

void Agent::step(Simulation &sim)
{
    if (!mIdle) {
        int patryId = (*mSelectionPolicy).select(sim.getGraph(),mPartyId);
        if (patryId != -1)
            sim.getParty(patryId).incomeOffer(sim.getParty(mPartyId).getCoalition());
        else 
            mIdle = true;
    }
}

// clone the agent when a party joined the coalition
Agent Agent::clone(int agentId, int partyId) const
{
    return Agent(agentId, partyId, (*mSelectionPolicy).clone());
}