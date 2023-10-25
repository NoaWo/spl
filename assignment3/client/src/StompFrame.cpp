#include "../include/StompFrame.h"
#include "../include/StompProtocol.h"

#include <string>
#include <iostream>
#include <vector>
#include <map>
#include <boost/asio.hpp>
#include <sstream>

using std::string;

StompFrame::StompFrame(string command) : command_(command), headers_(), body_("") {}

void StompFrame::addHeader(string key, string value) {
    headers_[key] = value;
}

void StompFrame::setBody(string body) {
    body_ = body;
}

const string StompFrame::getCommand() const {
    return command_;
}

bool StompFrame::hasHeader(std::string key) const {
    return (headers_.count(key) > 0);
}

// assume key exist in headers_
std::string StompFrame::getHeaderValue(std::string key) const {
    if (headers_.count(key) == 0) {
        // should not enter here!
        std::cout << "ERROR" << std::endl;
    }
    return headers_.at(key);
}

bool StompFrame::hasBody() const {
    return (body_ != "");
}

string StompFrame::getBody() const {
    return body_;
}

string StompFrame::toString() const {
    string output = command_ + "\n";
    for (const auto& keyval : headers_) {
        output += keyval.first + ":" + keyval.second + "\n";
    }
    output += "\n";
    if (hasBody()) 
        output += body_ + "\n";
    return output;
}

StompFrame StompFrame::stringToFrame(std::string str) {
    // Split the string into lines
    std::vector<string> lines = StompProtocol::split(str, '\n');

    // command part
    string command = lines[0];
    StompFrame frame = StompFrame(command);

    // headers part
    size_t i = 1;
    while (i < lines.size() && lines[i] != "") {
        size_t split = lines[i].find(':');
        if (split == string::npos) {
            break;
        }
        string key = lines[i].substr(0, split);
        string value = lines[i].substr(split + 1);
        frame.addHeader(key, value);
        ++i;
    }

    ++i;
    // body part
    string body = "";
    for (; i < lines.size(); ++i) {
        body += lines[i];
        if (i < lines.size() - 1) // if not the last line
            body += "\n";
    }
    if (body != "")
        frame.setBody(body);

    return frame;
}