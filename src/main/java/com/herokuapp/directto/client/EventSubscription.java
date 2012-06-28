package com.herokuapp.directto.client;

import java.util.*;

/**
 * @author Ryan Brainard
 */
public final class EventSubscription {

    public static enum Event {
        DEPLOY_PRE_VERIFICATION_START,
        DEPLOY_PRE_VERIFICATION_END,
        DEPLOY_START,
        UPLOAD_START,
        UPLOAD_END,
        POLL_START,
        POLLING,
        POLL_END,
        DEPLOY_END
    }

    public static interface Subscriber {
        void handle(Event event);
    }

    private final Map<Event, Set<Subscriber>> subscribers = new EnumMap<Event, Set<Subscriber>>(Event.class);

    void announce(Event event) {
        if (subscribers.containsKey(event)) {
            for (Subscriber subscriber : subscribers.get(event)) {
                subscriber.handle(event);
            }
        }
    }

    public EventSubscription subscribe(Event event, Subscriber subscriber) {
        return subscribe(EnumSet.of(event), subscriber);
    }

    public EventSubscription subscribe(EnumSet<Event> events, Subscriber subscriber) {
        for (Event event : events) {
            if (!subscribers.containsKey(event)) {
                subscribers.put(event, new HashSet<Subscriber>());
            }
            subscribers.get(event).add(subscriber);
        }

        return this;
    }
}
