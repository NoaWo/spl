#pragma once
#include <vector>

using std::vector;

class JoinPolicy {
    public:
        virtual int Join (const vector<int>& offers, const vector<int>& numMandatByCoalition) const = 0;
        virtual JoinPolicy* clone() const = 0;
        virtual ~JoinPolicy() = default;
};

class MandatesJoinPolicy : public JoinPolicy {
    public:
        int Join (const vector<int>& offers, const vector<int>& numMandatByCoalition) const override;
        JoinPolicy* clone() const override;
};

class LastOfferJoinPolicy : public JoinPolicy {
    public:
        int Join (const vector<int>& offers , const vector<int>& numMandatByCoalition) const override;
        JoinPolicy* clone() const override;
};