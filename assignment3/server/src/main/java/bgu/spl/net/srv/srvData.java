package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class srvData<T> {

    private Map<Integer, User> idToUser;
    private Map<String, User> usernameToUser;
    private Map<String, List<Integer>> channelToSubscribers; // subscribers by connectionId
    private AtomicInteger nextMessageId;

    public srvData() {

        this.idToUser = new ConcurrentHashMap<>();
        this.usernameToUser = new ConcurrentHashMap<>();
        this.channelToSubscribers = new ConcurrentHashMap<>();
        nextMessageId = new AtomicInteger();
    }

    // return User of username or null if there is no such User
    public User findUserbyUsername(String username) {
        return usernameToUser.get(username);
    }

    // return User of connectionId or null if there is no such User
    public User findUserByConnectionId(int connectionId) {
        return idToUser.get(connectionId);
    }

    public void addNewUser(User newUser) {
        idToUser.put(newUser.getConnectionId(), newUser);
        usernameToUser.put(newUser.getUsername(), newUser);
    }

    public void connectOldUser(int connectionId, User oldUser) {
        idToUser.put(connectionId, oldUser);
        oldUser.newConnect(connectionId);
    }

    // return list of subscribers (by connection id) of channel or null if there is no such channel 
    public List<Integer> getSubscribers(String channel) {
        return channelToSubscribers.get(channel);
    }

    // return subscriptionId of connectionId user to channel or null if there is no such subscription
    public Integer getSubscriptionId(int connectionId, String channel) {
        return idToUser.get(connectionId).getSubscriptionId(channel);
    }

    // generate and return the next messageId 
    public int getNextMessageId() {
        return nextMessageId.getAndIncrement();
    }
    
    // open new channel
    public void addNewChannel(String channel, int connectionId, int subscriptionId) {
        channelToSubscribers.put(channel, new LinkedList<>());
        addSubscriber(channel, connectionId, subscriptionId);
    }

    public void addSubscriber(String channel, int connectionId, int subscriptionId){
        List<Integer> subscribers = channelToSubscribers.get(channel);
        synchronized (subscribers) {
            subscribers.add(connectionId);
        }
        idToUser.get(connectionId).addSubscription(channel, subscriptionId);
    }

    public void removeSubscriber(int connectionId, String channel) {
        Integer id = connectionId;
        List<Integer> subscribers = channelToSubscribers.get(channel);
        synchronized (subscribers) {
            subscribers.remove(id);
        }
    }

    // user discconected
    public void removeUser(int connectionId) {
        idToUser.remove(connectionId);
    }

}