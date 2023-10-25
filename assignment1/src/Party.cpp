#include "Party.h"
#include "JoinPolicy.h"
#include "Simulation.h"
#include <iostream>

Party::Party(int id, string name, int mandates, JoinPolicy *jp) : mId(id), mName(name), mMandates(mandates), mJoinPolicy(jp), mState(Waiting), mTimer(0), mCoalitionID(-1), mOffers()
{
}

// Copy Constructor
Party::Party(const Party& other): mId(other.mId), mName(other.mName), mMandates(other.mMandates), mJoinPolicy((*other.mJoinPolicy).clone()), 
mState(other.mState), mTimer(other.mTimer), mCoalitionID(other.mCoalitionID) , mOffers(vector<int>(other.mOffers))
{
}

// Destructor
Party::~Party()
{
    if (mJoinPolicy != nullptr) {
        delete(mJoinPolicy);
        mJoinPolicy = nullptr;
    }
}

// Copy Assignment Operator
Party& Party::operator=(const Party& other)
{
    if (this != &other) {
        if (mJoinPolicy != nullptr) {
            delete(mJoinPolicy);
            mJoinPolicy = nullptr;
        } 
        mId = other.mId;
        mName = other.mName;
        mMandates = other.mMandates;
        mJoinPolicy = (*other.mJoinPolicy).clone();
        mState = other.mState;
        mTimer = other.mTimer;
        mCoalitionID = other.mCoalitionID;
        mOffers = other.mOffers;
    }
    return *this;
}

// Move Constructor
Party::Party(Party&& other): mId(other.mId), mName(other.mName), mMandates(other.mMandates), mJoinPolicy(other.mJoinPolicy), 
mState(other.mState), mTimer(other.mTimer), mCoalitionID(other.mCoalitionID) , mOffers(std::move(other.mOffers)) 
{
    other.mJoinPolicy = nullptr;
}

// Move Assignment Operator
Party& Party::operator=(Party&& other)
{
    if (this != &other) {
        if (mJoinPolicy != nullptr) {
            delete(mJoinPolicy);
            mJoinPolicy = nullptr;
        } 
        mId = other.mId;
        mName = other.mName;
        mMandates = other.mMandates;
        mJoinPolicy = other.mJoinPolicy;
        mState = other.mState;
        mTimer = other.mTimer;
        mCoalitionID = other.mCoalitionID;
        mOffers = std::move(other.mOffers);
        other.mJoinPolicy = nullptr;
    }
    return *this;
}

State Party::getState() const
{
    return mState;
}

void Party::setState(State state)
{
    mState = state;
}

int Party::getMandates() const
{
    return mMandates;
}

const string & Party::getName() const
{
    return mName;
}

void Party::step(Simulation &s)
{
    if (mState == CollectingOffers){
        mTimer++;
        if (mTimer == 3) { // need to join coalition
            vector<int> &mandatsByCoalition = s.getNumMandatByCoalition();
            int coalition = (*mJoinPolicy).Join(mOffers, mandatsByCoalition);
            mCoalitionID = coalition;
            s.getPartiesByCoalition()[coalition].push_back(mId);
            mandatsByCoalition[coalition] = mandatsByCoalition[coalition] + mMandates;
            mState = Joined;
            // clone the agent and add it to agents' vector
            int agentId = s.getAgents().size();
            s.getAgents().push_back((s.getAgents())[coalition].clone(agentId, mId));
        }   
    }
}

int Party::getCoalition() const
{
    return mCoalitionID;
}

const vector<int>& Party::getOffers() const
{
    return mOffers;
}

void Party::setCoalitionId(int coalitionId)
{
    mCoalitionID = coalitionId;
}

// add a new offer from coalition
void Party::incomeOffer(int coalitionID)
{
    mOffers.push_back(coalitionID);
    if (mState == Waiting)
        mState = CollectingOffers; 
}