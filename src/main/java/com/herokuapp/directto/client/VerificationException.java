package com.herokuapp.directto.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ryan Brainard
 */
public class VerificationException extends Exception {

    private final List<String> messages;

    private VerificationException(List<String> messages) {
        super(messages.toString());
        this.messages = Collections.unmodifiableList(messages);
    }

    public List<String> getMessages() {
        return messages;
    }

    protected static class Aggregator {
        private final List<String> messages = new ArrayList<String>();

        Aggregator addMessage(String msg) {
            messages.add(msg);
            return this;
        }

        void detonate() throws VerificationException {
            if (!messages.isEmpty()) {
                throw new VerificationException(messages);
            }
        }
    }
}
