package org.jdownloader.extensions.extraction.gui.bubble;

import java.util.HashMap;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.ExtractionListener;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleType;

public class ExtractionBubbleSupport extends BubbleType implements ExtractionListener {

    public ExtractionBubbleSupport(String bubbletype, BooleanKeyHandler bubbleEnabledIfArchiveExtractionIsInProgress) {
        super(bubbletype, bubbleEnabledIfArchiveExtractionIsInProgress);

    }

    @Override
    public void onExtractionEvent(ExtractionEvent event) {
        if (!getKeyHandler().isEnabled()) return;
        switch (event.getType()) {
        case START:
            BubbleNotify.getInstance().show(getBubble(event.getCaller()));
            getBubble(event.getCaller()).refresh(event);
            return;
        case CLEANUP:
            getBubble(event.getCaller()).refresh(event);
            getBubble(event.getCaller()).stop();
            return;
        default:
            getBubble(event.getCaller()).refresh(event);

        }
    }

    private HashMap<ExtractionController, ExtractionBubble> map = new HashMap<ExtractionController, ExtractionBubble>();

    private ExtractionBubble getBubble(ExtractionController caller) {

        synchronized (map) {
            ExtractionBubble ret = map.get(caller);
            if (ret == null) {
                ret = new ExtractionBubble(caller);
                map.put(caller, ret);
            }
            return ret;

        }

    }

}
