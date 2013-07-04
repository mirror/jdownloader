package org.jdownloader.api.myjdownloader.event;

import org.appwork.utils.event.SimpleEvent;

public class MyJDownloaderEvent extends SimpleEvent<Object, Object, MyJDownloaderEvent.Type> {

    public static enum Type {
        CONNECTION_STATUS_UPDATE
    }

    public MyJDownloaderEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}