package org.jdownloader.gui.notify;

import java.util.List;

import org.appwork.storage.config.handler.BooleanKeyHandler;

public class BasicBubbleSupport extends AbstractBubbleSupport {

    public BasicBubbleSupport(String label, BooleanKeyHandler keyhandler) {
        super(label, keyhandler);
    }

    @Override
    public List<Element> getElements() {
        return null;
    }

}
