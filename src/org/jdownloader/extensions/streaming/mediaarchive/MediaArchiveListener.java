package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.EventListener;

public interface MediaArchiveListener extends EventListener {

    void onPrepareQueueUpdated(MediaArchiveController caller);

}