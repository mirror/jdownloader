package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.utils.event.SimpleEvent;

public class MediaArchiveEvent extends SimpleEvent<MediaArchiveController, Object, MediaArchiveEvent.Type> {

    public static enum Type {
        PREPARER_QUEUE_UPDATE
    }

    public MediaArchiveEvent(MediaArchiveController caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}