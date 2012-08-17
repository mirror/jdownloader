package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;
import java.util.List;

public abstract class MediaListController<T extends MediaItem> {

    private MediaListEventSender eventSender;
    private ArrayList<T>         list;

    public MediaListEventSender getEventSender() {
        return eventSender;
    }

    protected MediaListController() {
        eventSender = new MediaListEventSender();
        list = new ArrayList<T>();
    }

    public List<T> getList() {
        synchronized (list) {
            return new ArrayList<T>(list);
        }
    }

    public void add(T node) {
        synchronized (list) {
            list.add(node);
        }
        fireContentChanged();

    }

    private void fireContentChanged() {
        getEventSender().fireEvent(new MediaListEvent(this, MediaListEvent.Type.CONTENT_CHANGED));
    }

}
