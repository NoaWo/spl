#pragma once

#include "../include/Game.h"

#include <vector>
#include <string>
#include <iostream>
#include <map>
#include <atomic>
#include <mutex>

// #ifndef screenMutex
// #define std::mutex screenMutex;
// #endif

extern std::mutex screenMutex;

class Event;
class StompFrame;
class ConnectionHandler;

class StompProtocol
{
private:

    std::string username_;

    std::atomic<bool> hasConnected_;
    std::atomic<bool> shouldTerminate_;

    std::map<std::string, std::pair<int, Game>> gameNameToSubIdGame_; // gameName to <subId, Game>

    std::map<std::string, std::pair<std::string, std::string>> receiptIdToCommand_; // receiptId to <command, gameName>

    int nextSubId_;
    int nextReceiptId_;

    //static std::mutex screenM_;

    StompFrame connect(std::string username, std::string password);
    StompFrame subscribe(std::string gameName);
    StompFrame unsubscribe(std::string game);
    StompFrame disconnect();

    void send(std::string team_a_name, std::string team_b_name, std::vector<Event> events, std::vector<StompFrame> &framesToSend); 

    void handleSummary(std::string gameName, std::string user, std::string file);

    StompFrame eventToFrame(Event event);
    static std::vector<std::string> framesToStrings(std::vector<StompFrame>);

    void handleError(StompFrame frame);
    bool handleReceipt(StompFrame frame);
    void handleMessage(StompFrame frame);
    void incomeMessageParse(std::string gameName, std::string body);
    
public:

    StompProtocol();

    static std::vector<std::string> split(const std::string &s, char delimeter);

    ConnectionHandler* loginProcess(std::string input);
    std::vector<std::string> inputProcess(std::string input);
    // return true iff the frame is answer on login\logout or error message
    bool incomeProcess(std::string framestr);

    void hasConnect();
    bool shouldTerminate();
    bool isConnected();

    void terminate();
};