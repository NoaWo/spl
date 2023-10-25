#pragma once

#include "../include/GameUpdates.h"

#include <iostream>
#include <string>
#include <map>

class Game final
{
private:
    std::string teamA_;
    std::string teamB_;
    std::map<std::string, GameUpdates> usernameToGameUpdates_;

public:
    Game(); // default constructor
    Game(std::string teamA, std::string teamB);
    const std::string getTeamA() const;
    const std::string getTeamB() const;
    GameUpdates &getGameUpdates(std::string username);
};