#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include "../include/Game.h"

#include <stdlib.h>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>

using namespace std;

mutex m;
std::condition_variable cv;
atomic<bool> getAnswer {false};
mutex screenMutex;

void incomeSrvThread(ConnectionHandler* connectionHandler, StompProtocol& protocol) {
    while (!protocol.shouldTerminate()) {
        string answer;
        // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
        if (!(*connectionHandler).getFrameAscii(answer, '\0')) { // get answer from server - 
            {
                lock_guard<mutex> lock(screenMutex);
                std::cout << "Disconnected. Exiting...\n" << endl;
            }
            protocol.terminate();
            // end the second thread connection
            return;
        }
        int len = answer.length();
        // A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
        // we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
        answer.resize(len - 1);
        bool toWake = protocol.incomeProcess(answer);
        if (toWake) { // logged in or logged out or error
            {
                lock_guard<mutex> lock(m);
                getAnswer = true;
            }
            cv.notify_all();
        }
    }
}


int main(int argc, char *argv[]) {
    const short bufsize = 1024;
    char buf[bufsize];
    bool lineDidntProcess = false;
    
    while (1) {
        StompProtocol protocol;
        ConnectionHandler* connectionHandler = nullptr;
        thread threadObj;

        // first log in
        // login loop
        while (!protocol.isConnected() && !protocol.shouldTerminate()) {
            if (!lineDidntProcess)
                cin.getline(buf, bufsize); // read from keybord
            else {
                lineDidntProcess = false;
            }
            string line(buf);

            // handle login
            connectionHandler = protocol.loginProcess(line);
            if (connectionHandler == nullptr) {
                continue;
            }

            auto fn = bind(incomeSrvThread, connectionHandler, ref(protocol));
            threadObj = thread(fn);
            // wait for connected/error frame
            unique_lock<mutex> lock(m);
            if (!getAnswer) {
                cv.wait(lock);
            }
            getAnswer = false;
        }
        // login successful

        //main loop
        while (!protocol.shouldTerminate()) {

            cin.getline(buf, bufsize); // read from keybord
            if (protocol.shouldTerminate()) {
                getAnswer = false;
                lineDidntProcess = true;
                break;
            }
            string line(buf);
            vector<string> toSend = protocol.inputProcess(line);
            for (string frame : toSend) {
                if (!(*connectionHandler).sendFrameAscii(frame, '\0')) { // send the frame
                    {
                        lock_guard<mutex> lock(screenMutex);
                        std::cout << "Disconnected. Exiting...\n" << endl;
                    }
                    protocol.terminate();
                    break;
                }
            }
            // if command was disconnect 
            if (line == "logout") {
                unique_lock<mutex> lock(m);
                if (!getAnswer) {
                    cv.wait(lock);
                }
                getAnswer = false;
            }

        }
        threadObj.join();
        delete connectionHandler;
        connectionHandler = nullptr;
    }
    return 0;
}