package org.jdownloader.extensions.extraction;

import java.util.EventListener;

public interface ExtractionListener extends EventListener {
    public void onExtractionEvent(ExtractionEvent event);
}