#include "../include/Game.h"
#include "../include/GameUpdates.h"

#include <iostream>
#include <string>
#include <map>

Game::Game() : teamA_(), teamB_(), usernameToGameUpdates_() {}

Game::Game(std::string teamA, std::string teamB) : teamA_(teamA), teamB_(teamB), usernameToGameUpdates_() {
}

const std::string Game::getTeamA() const {
    return teamA_;
}

const std::string Game::getTeamB() const {
    return teamB_;
}

GameUpdates &Game::getGameUpdates(std::string username) {
    GameUpdates &output = usernameToGameUpdates_[username];
    return output;
}

