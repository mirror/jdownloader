package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.EventListener;

public interface MediaListListener extends EventListener {

    void onContentChanged(MediaListController<?> caller);

}