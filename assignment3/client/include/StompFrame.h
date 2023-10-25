#pragma once

#include <string>
#include <iostream>
#include <map>

using std::string;

class StompFrame {

private:
    const string command_;
    std::map<string, string> headers_;
    string body_;

public:
    StompFrame(string command);

    void addHeader(string key, string value);
    void setBody(string body);

    const string getCommand() const;
    bool hasHeader(string key) const;
    string getHeaderValue(string key) const;
    bool hasBody() const;
    std::string getBody() const;

    string toString() const;

    static StompFrame stringToFrame(std::string str);
};