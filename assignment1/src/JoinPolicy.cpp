#include "JoinPolicy.h"
#include <vector>

using std::vector;

// return the coalition's Id with the most mandats
int MandatesJoinPolicy::Join (const vector<int>& offers, const vector<int>& numMandatByCoalition) const
{
    int leadCoalition = offers[0];
    for (unsigned int i=1; i<offers.size(); i++) {
        if (numMandatByCoalition[leadCoalition]<numMandatByCoalition[offers[i]])
            leadCoalition = offers[i];
    }
    return leadCoalition;
}

// return the coalition's Id that offered last
int LastOfferJoinPolicy::Join (const vector<int>& offers, const vector<int>& numMandatByCoalition) const
{
    return offers[offers.size()-1];
}

// return new inastance of MandatesJoinPolicy
JoinPolicy* MandatesJoinPolicy::clone() const
{
    return new MandatesJoinPolicy();
}

// return new inastance of EdgeWeightSelectionPolicy
JoinPolicy* LastOfferJoinPolicy::clone() const
{
    return new LastOfferJoinPolicy();
}