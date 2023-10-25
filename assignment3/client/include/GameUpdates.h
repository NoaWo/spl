#pragma once

#include <iostream>
#include <map>
#include <vector>
#include <string>
#include <mutex>

class GameUpdates final
{
private:
    std::map<std::string, std::string> genralUpdates_;
    std::map<std::string, std::string> teamAUpdates_;
    std::map<std::string, std::string> teamBUpdates_;

    std::map<int, std::pair<std::string,std::string>> timeToEvent1H_; // first half. event(eventName, description)
    std::map<int, std::pair<std::string,std::string>> timeToEvent2H_; // second half. event(eventName, description)

public:
    std::mutex mut_;

    GameUpdates();
    GameUpdates(const GameUpdates &other);

    void addGeneralUpdate(std::string s1, std::string s2);
    void addTeamAUpdate(std::string s1, std::string s2);
    void addTeamBUpdate(std::string s1, std::string s2);
    void addEventTo1H(int time, std::string eventName, std::string description);
    void addEventTo2H(int time, std::string eventName, std::string description);

    bool isBeforeHalfTime();

    const std::map<std::string, std::string> &getGenralUpdates() const;
    const std::map<std::string, std::string> &getTeamAUpdates() const;
    const std::map<std::string, std::string> &getTeamBUpdates() const;

    const std::map<int, std::pair<std::string,std::string>> &getFirstHalfEvents() const;
    const std::map<int, std::pair<std::string,std::string>> &getSecondHalfEvents() const;
};