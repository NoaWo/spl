#include "../include/GameUpdates.h"

#include <iostream>
#include <map>
#include <vector>
#include <string>

GameUpdates::GameUpdates() : genralUpdates_(), teamAUpdates_(), teamBUpdates_(), timeToEvent1H_(),
                            timeToEvent2H_(), mut_() {
}

GameUpdates::GameUpdates(const GameUpdates &other) : genralUpdates_(other.genralUpdates_), teamAUpdates_(other.teamAUpdates_), 
                            teamBUpdates_(other.teamBUpdates_), timeToEvent1H_(other.timeToEvent1H_),
                            timeToEvent2H_(other.timeToEvent2H_), mut_() {}

void GameUpdates::addGeneralUpdate(std::string s1, std::string s2) {
    genralUpdates_[s1] = s2;
}

void GameUpdates::addTeamAUpdate(std::string s1, std::string s2) {
    teamAUpdates_[s1] = s2;
}

void GameUpdates::addTeamBUpdate(std::string s1, std::string s2) {
    teamBUpdates_[s1] = s2;
}

void GameUpdates::addEventTo1H(int time, std::string eventName, std::string description) {
    timeToEvent1H_[time] = std::make_pair(eventName, description);
}

void GameUpdates::addEventTo2H(int time, std::string eventName, std::string description) {
    timeToEvent2H_[time] = std::make_pair(eventName, description);
}

bool GameUpdates::isBeforeHalfTime() {
    if (genralUpdates_["before halftime"] == "true") {
        return true;
    }
    return false;
}

const std::map<std::string, std::string>& GameUpdates::getGenralUpdates() const {
    return genralUpdates_;
}

const std::map<std::string, std::string>& GameUpdates::getTeamAUpdates() const {
    return teamAUpdates_;
}

const std::map<std::string, std::string>& GameUpdates::getTeamBUpdates() const {
    return teamBUpdates_;
}

const std::map<int, std::pair<std::string,std::string>>& GameUpdates::getFirstHalfEvents() const {
    return timeToEvent1H_;
}

const std::map<int, std::pair<std::string,std::string>>& GameUpdates::getSecondHalfEvents() const {
    return timeToEvent2H_;
}