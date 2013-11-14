package org.jdownloader.gui.notify;

import org.appwork.storage.config.handler.BooleanKeyHandler;

public class BubbleType {

    private String label;

    public String getLabel() {
        return label;
    }

    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }

    private BooleanKeyHandler keyHandler;

    public BubbleType(String label, BooleanKeyHandler keyhandler) {
        this.label = label;
        this.keyHandler = keyhandler;
    }

}
