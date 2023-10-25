#pragma once
#include <string>
#include <vector>

using std::string;
using std::vector;

class JoinPolicy;
class Simulation;

enum State
{
    Waiting,
    CollectingOffers,
    Joined
};

class Party
{
public:
    Party(int id, string name, int mandates, JoinPolicy *); 
    //rule of 5
    Party(const Party& other);
    ~Party();
    Party& operator=(const Party& other); 
    Party(Party&& other);
    Party& operator=(Party&& other); 

    State getState() const;
    void setState(State state);
    int getMandates() const;
    void step(Simulation &s);
    const string &getName() const;
    void incomeOffer(int coalitionID);
    int getCoalition() const;
    void setCoalitionId(int coalitionId);
    const vector<int>& getOffers() const;

private:
    int mId;
    string mName;
    int mMandates;
    JoinPolicy *mJoinPolicy;
    State mState;
    int mTimer;
    int mCoalitionID;
    vector<int> mOffers;
};
