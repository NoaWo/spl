#include "../include/StompProtocol.h"
#include "../include/StompFrame.h"
#include "../include/ConnectionHandler.h"
#include "../include/Game.h"
#include "../include/GameUpdates.h"
#include "../src/event.cpp"

#include <stdlib.h>
#include <iostream>
#include <string>
#include <vector>
#include <boost/asio.hpp>
#include <sstream>
#include <fstream>
#include <map>

using namespace std;

StompProtocol::StompProtocol() : username_(), hasConnected_(false), shouldTerminate_(false), gameNameToSubIdGame_(), 
                                receiptIdToCommand_(), nextSubId_(0), nextReceiptId_(0) {}

vector<string> StompProtocol::split(const string& s, char delimiter) {
   vector<string> output;
   string token;
   for (auto c : s) {
        if(c == delimiter)
        {
            output.push_back(token);
            token = ""; //reset token
        }
        else token += c;
    }
    output.push_back(token); 
    return output;
}

bool StompProtocol::shouldTerminate() {
    return shouldTerminate_;
}

bool StompProtocol::isConnected() {
    return hasConnected_;
}

ConnectionHandler* StompProtocol::loginProcess(string input) {

    vector<string> words = StompProtocol::split(input, ' ');
    string command = words[0];

    if (command != "login") {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "You must log in first. Try again" << endl;
        }
        return nullptr;
    }
    if (words.size() != 4) {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "Illegal arguments for login. Try again" << endl;
        }
        return nullptr;
    }
    string hostport = words[1];
    string username = words[2];
    string password = words[3];

    vector<string> hp = StompProtocol::split(hostport, ':');
    string host = hp[0];
    if (hp.size() != 2) {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "Illegal arguments for login. Try again" << endl;
        }
        return nullptr;
    }
    short port = stoi(hp[1]);

    // try connect 
    ConnectionHandler* connectionHandler = new ConnectionHandler(host, port);
    if (!(*connectionHandler).connect()) { // problem in connection
        {
            lock_guard<mutex> lock(screenMutex);
            cerr << "Could not connect to server"  << endl;
            cerr << "Cannot connect to " << host << ":" << port << endl;
        }
        delete connectionHandler;
        return nullptr;
    }
    
    // try login
    // send CONNECT frame
    username_ = username;
    StompFrame connectF = connect(username, password);
    string toSend = connectF.toString();
    if (!(*connectionHandler).sendFrameAscii(toSend, '\0')) { // send the frame
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "Disconnected. Exiting...\n" << endl;
        }
        delete connectionHandler;
        return nullptr;
    }
    return connectionHandler;
}

// CONNECT frame
StompFrame StompProtocol::connect(string username, string password) {
    StompFrame frame = StompFrame("CONNECT");
    frame.addHeader("accept-version", "1.2");
    frame.addHeader("host", "stomp.cs.bgu.ac.il");
    frame.addHeader("login", username);
    frame.addHeader("passcode", password);
    return frame;
}

vector<string> StompProtocol::inputProcess(std::string input) {

    vector<StompFrame> framesToSend;

    // split input with " "
    vector<string> words = StompProtocol::split(input, ' ');

    // check the command
   const string command = words[0];
    if (command == "login") {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "The client is already logged in, log out before trying again." << endl;
        }
        return vector<string>();
    }
    else if (command == "join") {
        if (gameNameToSubIdGame_.count(words[1]) > 0) {
            {
                lock_guard<mutex> lock(screenMutex);
                cout << "You are already joined to this game." << endl;
            }
            return vector<string>();
        }
        if (words.size() != 2 || StompProtocol::split(words[1], '_').size() != 2){
            {
                lock_guard<mutex> lock(screenMutex);
                cout << "Illegal arguments for join" << endl;
            }
            return vector<string>();
        }
        // send SUBSCRIBE frame
        framesToSend.push_back(subscribe(words[1]));
    }
    else if (command == "exit") {
        if (gameNameToSubIdGame_.count(words[1]) == 0) {
            {
                lock_guard<mutex> lock(screenMutex);
                cout << "You did not join to this game." << endl;
            }
            return vector<string>();
        }
        // send UNSUBSCRIBE frame
        framesToSend.push_back(unsubscribe(words[1])); 
    }
    else if (command == "report") {
        try {
            string json = words[1];
            names_and_events nne = parseEventsFile(json);
            send(nne.team_a_name, nne.team_b_name, nne.events, framesToSend);
        } catch (exception e) {
            {
                lock_guard<mutex> lock(screenMutex);
                cout << "Illegal arguments for report" << endl;
            }
            return vector<string>();
        }
    }
    else if (command == "summary") {
        if (words.size() != 4) {
            {
                lock_guard<mutex> lock(screenMutex);
                cout << "Illegal arguments for summary. Try again" << endl;
            }
            return vector<string>();
        }
        string gameName = words[1];
        string user = words[2];
        string file = words[3];

        handleSummary(gameName, user, file);
        return vector<string>();
    }
    else if (command == "logout") { 
        // send DISCONNECT frame
        framesToSend.push_back(disconnect());
    }
    else {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "Illegal command. Try again" << std::endl;
        }
        return vector<string>();
    }
    return StompProtocol::framesToStrings(framesToSend);
}

// SUBSCRIBE frame
StompFrame StompProtocol::subscribe(string gameName) {
    string command = "SUBSCRIBE";
    StompFrame frame = StompFrame(command);
    frame.addHeader("destination", gameName);
    
    vector<string> ab = StompProtocol::split(gameName, '_');
    gameNameToSubIdGame_[gameName] = make_pair(nextSubId_, Game(ab[0], ab[1]));
    frame.addHeader("id", to_string(nextSubId_));
    nextSubId_++;

    string receipt = to_string(nextReceiptId_);
    receiptIdToCommand_[receipt] = make_pair(command, gameName);
    frame.addHeader("receipt", receipt);
    nextReceiptId_++;

    return frame;
}

// UNSUBSCRIBE frame
StompFrame StompProtocol::unsubscribe(std::string gameName) {
    string command = "UNSUBSCRIBE";
    StompFrame frame = StompFrame(command);

    int subId = get<0>(gameNameToSubIdGame_[gameName]);
    frame.addHeader("id", to_string(subId));

    gameNameToSubIdGame_.erase(gameName);

    string receipt = to_string(nextReceiptId_);
    receiptIdToCommand_[receipt] = make_pair(command, gameName);
    frame.addHeader("receipt", receipt);
    nextReceiptId_++;

    return frame;
}

// DISCONNECT frame
StompFrame StompProtocol::disconnect() {
    string command = "DISCONNECT";
    StompFrame frame = StompFrame(command);

    string receipt = to_string(nextReceiptId_);
    receiptIdToCommand_[receipt] = make_pair(command, "");
    frame.addHeader("receipt", receipt);
    nextReceiptId_++;

    return frame;
}

void StompProtocol::send(string team_a_name, string team_b_name, vector<Event> events, vector<StompFrame> &framesToSend) {
    for (Event event : events) {
        StompFrame frame = eventToFrame(event);
        framesToSend.push_back(frame);
    }
}

StompFrame StompProtocol::eventToFrame(Event event) {
    // SEND frame
    StompFrame frame = StompFrame("SEND");
    frame.addHeader("destination", event.get_team_a_name() + "_" + event.get_team_b_name());
    std::string body;

    body = "user:"+ username_ + "\n";

    body += "team a:" + event.get_team_a_name() + "\n";
    body += "team b:" + event.get_team_b_name() + "\n";

    body += "event name:" + event.get_name()+ "\n";
    body += "time:" + std::to_string(event.get_time())+ "\n";
    body += "general game updates:\n";

    for (const auto elem : event.get_game_updates() ) {
        body += elem.first + ":" + elem.second + "\n";
    }

    body += "team a updates:\n";
    for (const auto elem : event.get_team_a_updates()) {
        body += elem.first + ":" + elem.second + "\n";
    }

    body += "team b updates:\n";
    for (const auto elem : event.get_team_b_updates()) {
        body += elem.first + ":" + elem.second + "\n";
    }

    body += "description:\n";
    body += event.get_discription();

    frame.setBody(body); 

    return frame;
}

vector<string> StompProtocol::framesToStrings(vector<StompFrame> frames) {
    vector<string> output;
    for (StompFrame frame : frames) {
        output.push_back(frame.toString());
    }
    return output;
}

void StompProtocol::handleSummary(string gameName, string user, string file) {
    if (gameNameToSubIdGame_.count(gameName) == 0) {
        {
            lock_guard<mutex> lock(screenMutex);
            cerr << "You are not subscribe to game " << gameName << endl;
        }
        return;
    }
    ofstream outfile;
    outfile.open(file);
    if (!outfile) {
        {
            lock_guard<mutex> lock(screenMutex);
            cerr << "Error opening file " << file << endl;
        }
        return;
    }
    Game game = get<1>(gameNameToSubIdGame_[gameName]);
    const string teamA = game.getTeamA();
    const string teamB = game.getTeamB();
    GameUpdates &gameUp = game.getGameUpdates(user);
    outfile << teamA << " vs " << teamB << "\n";
    outfile << "Game stats:\n";
    outfile << "General stats:\n";
    {
        lock_guard<mutex> lock(gameUp.mut_);
        map<string, string> m = gameUp.getGenralUpdates();
        for (map<string, string>::iterator it = m.begin(); it != m.end(); it++) {
            outfile << it->first << ": " << it->second << "\n";
        }
        outfile << teamA << " stats:\n";
        m = gameUp.getTeamAUpdates();
        for (map<string, string>::iterator it = m.begin(); it != m.end(); it++) {
            outfile << it->first << ": " << it->second << "\n";
        }
        outfile << teamB << " stats:\n";
        m = gameUp.getTeamBUpdates();
        for (map<string, string>::iterator it = m.begin(); it != m.end(); it++) {
            outfile << it->first << ": " << it->second << "\n";
        }
        outfile << "Game event reports:\n";
        map<int, pair<string, string>> m1 = gameUp.getFirstHalfEvents();
        for (map<int, pair<string, string>>::iterator it = m1.begin(); it != m1.end(); it++) {
            int time = it->first;
            pair<string, string> p = it->second;
            string eventName = get<0>(p);
            string description = get<1>(p);
            outfile << to_string(time) << " - " << eventName << ":\n\n";
            outfile << description << "\n\n\n";
        }
        m1 = gameUp.getSecondHalfEvents();
        for (map<int, pair<string, string>>::iterator it = m1.begin(); it != m1.end(); it++) {
            int time = it->first;
            pair<string, string> p = it->second;
            string eventName = get<0>(p);
            string description = get<1>(p);
            outfile << to_string(time) << " - " << eventName << ":\n\n";
            outfile << description << "\n\n\n";
        }
    }
    outfile.close();
    return;
}

bool StompProtocol::incomeProcess(string framestr) {
    StompFrame frame = StompFrame::stringToFrame(framestr);

    string command = frame.getCommand();
    if (command == "CONNECTED") {
        hasConnect();
        return true;
    }
    else if (command == "ERROR") {
        handleError(frame);
        return true;
    }
    else if (command == "RECEIPT") {
        return handleReceipt(frame);
    }
    else if (command == "MESSAGE") {
        handleMessage(frame);
        return false;
    }
    return false;
}

void StompProtocol::hasConnect() {
    hasConnected_ = true;
    {
        lock_guard<mutex> lock(screenMutex);
        cout << "Login successful" << endl;
    }
}

void StompProtocol::handleError(StompFrame frame) {
    {
        lock_guard<mutex> lock(screenMutex);
        cout << "ERROR" << endl;
        cout << frame.getHeaderValue("message") << endl;
        if (frame.hasBody()) {
            cout << "The problem is:" << endl;
            cout << frame.getBody() << endl;
        }
        if (frame.hasHeader("receipt-id")) {
            string receiptId = frame.getHeaderValue("receipt-id");
            cout << "The ERROR is related to frame " << frame.getHeaderValue("receipt-id") << endl;
            string command = get<0>(receiptIdToCommand_[receiptId]);
            if (get<1>(receiptIdToCommand_[receiptId]) != "") {
                cout << "ERROR in : Tried to perform " << command << " with " << get<1>(receiptIdToCommand_[receiptId]) << endl;
            }
            else {
                cout << "ERROR in : Tried to perform " << command << endl;
            }
        }
    }
    terminate();
}

bool StompProtocol::handleReceipt(StompFrame frame) {
    string receiptId = frame.getHeaderValue("receipt-id");
    string command = get<0>(receiptIdToCommand_[receiptId]);
    bool output = false;
    if (command == "SUBSCRIBE") {
        lock_guard<mutex> lock(screenMutex);
        cout << "Joined channel " << get<1>(receiptIdToCommand_[receiptId]) << endl;
    }
    else if (command == "UNSUBSCRIBE") {
        lock_guard<mutex> lock(screenMutex);
        cout << "Exited channel " << get<1>(receiptIdToCommand_[receiptId]) << endl;
    }
    else if (command == "DISCONNECT") {
        {
            lock_guard<mutex> lock(screenMutex);
            cout << "Logout successful" << endl;
        }
        terminate();
        output = true;
    }
    receiptIdToCommand_.erase(receiptId);
    return output;
}

void StompProtocol::handleMessage(StompFrame frame) {
    string destination = frame.getHeaderValue("destination");
    string body = frame.getBody();
    incomeMessageParse(destination, body);
}

void StompProtocol::incomeMessageParse(string gameName, string body) {
    vector<string> lines = split(body, '\n');

    string username = StompProtocol::split(lines[0], ':')[1];
    string teamA = StompProtocol::split(lines[1], ':')[1];
    string teamB = StompProtocol::split(lines[2], ':')[1];
    string eventName = StompProtocol::split(lines[3], ':')[1];
    string strTime = StompProtocol::split(lines[4], ':')[1];
    int time =  stoi(strTime);

    //find the user that send the messge and put the data in the right place
    if (gameNameToSubIdGame_.count(gameName) == 0) {
        // should not enter here
        {
            lock_guard<mutex> lock(screenMutex);
            cerr << "ERROR" << endl;
        }
        return;
    }
    GameUpdates &currUpdates = get<1>(gameNameToSubIdGame_[gameName]).getGameUpdates(username);

    lock_guard<mutex> lock(currUpdates.mut_);

    unsigned int line = 6;

    while (lines[line] != "team a updates:" && line < lines.size()) {
        vector<string> kv = StompProtocol::split(lines[line], ':');
        currUpdates.addGeneralUpdate(kv[0], kv[1]);
        line++;
    }
    line++;
    while (lines[line] != "team b updates:" && line < lines.size()) {
        vector<string> kv = StompProtocol::split(lines[line], ':');
        currUpdates.addTeamAUpdate(kv[0], kv[1]);
        line++;
    }
    line++;
    while (lines[line] != "description:" && line < lines.size()) {
        vector<string> kv = StompProtocol::split(lines[line], ':');
        currUpdates.addTeamBUpdate(kv[0], kv[1]);
        line++;
    }
    line++;
    // descripion
    string description = "";
    while (line < lines.size()) {
        description += lines[line];
        if (line < lines.size() - 1) { // if not last line
            description += "\n";
        }
        line++;
    }

    if (currUpdates.isBeforeHalfTime()) {
        currUpdates.addEventTo1H(time, eventName, description);
    }
    else {
        currUpdates.addEventTo2H(time, eventName, description);
    }
}

void StompProtocol::terminate() {
    shouldTerminate_ = true;
}