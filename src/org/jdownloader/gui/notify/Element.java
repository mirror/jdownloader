package org.jdownloader.gui.notify;

import org.appwork.storage.config.handler.BooleanKeyHandler;

public class Element {
    private BooleanKeyHandler keyhandler;

    public BooleanKeyHandler getKeyhandler() {
        return keyhandler;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * @param keyhandler
     * @param label
     * @param icon
     */
    public Element(BooleanKeyHandler keyhandler, String label, String icon) {
        super();
        this.keyhandler = keyhandler;
        this.label = label;
        this.icon = icon;
    }

    private String label;
    private String icon;
}