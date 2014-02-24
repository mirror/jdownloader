package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class MessageData implements Storable {
    private String message;
    private String data;

    public MessageData(/* storable */) {

    }

    public MessageData(String text, String data) {
        this.message = text;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
