package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.utils.event.SimpleEvent;

public class MediaListEvent extends SimpleEvent<MediaListController<?>, Object, MediaListEvent.Type> {

    public static enum Type {
        CONTENT_CHANGED
    }

    public MediaListEvent(MediaListController<?> caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}