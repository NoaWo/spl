package bgu.spl.net.srv;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class User {
    
    private final String username; 
    private final String password;
    private volatile boolean isConnect;
    private int connectionId;
    private Map<String, Integer> channelToSubscriptionId;

    public User(String username , String password ,int connectionId) {

        this.username = username;
        this.password = password;
        this.isConnect = true;
        this.connectionId = connectionId;
        this.channelToSubscriptionId = new HashMap<>();
    }

    public void newConnect(int connectionId) {
        this.connectionId = connectionId;
        this.isConnect = true;
    }

    public void disconnect() {
        clearChannels();
        this.isConnect = false;
        this.connectionId = -1;
    }

    public void addSubscription(String channel, int subscriptionId) {
        channelToSubscriptionId.put(channel, subscriptionId);
    }

    // unsubscribe and return the channel of subscriptionId or null if there is no such subscriptionId
    public String unsubscribe(int subscriptionId) {

        String channel = null;
        for (String c : channelToSubscriptionId.keySet()) {
            if (channelToSubscriptionId.get(c) == subscriptionId) {
                channel = c;
                break;
            }
        }

        if (channel == null) {
            return null;
        }
        channelToSubscriptionId.remove(channel);
        return channel;
    }

    // clear all subscriptions
    private void clearChannels() {
        channelToSubscriptionId.clear();
    }

    public int getConnectionId() {
        return this.connectionId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean getIsConnect() {
        return this.isConnect;
    }

    public Set<String> getChannels() {
        return channelToSubscriptionId.keySet();
    }

    // return subscriptionId of channel or null if user is not subscribed to channel
    public Integer getSubscriptionId(String channel) {
        return channelToSubscriptionId.get(channel);
    }
}