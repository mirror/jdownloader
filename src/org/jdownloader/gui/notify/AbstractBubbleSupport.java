package org.jdownloader.gui.notify;

import java.util.List;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;

public abstract class AbstractBubbleSupport {
    
    private String label;
    
    public String getLabel() {
        return label;
    }
    
    abstract public List<Element> getElements();
    
    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }
    
    protected BooleanKeyHandler keyHandler;
    
    public AbstractBubbleSupport(String label, BooleanKeyHandler keyhandler) {
        this.label = label;
        this.keyHandler = keyhandler;
    }
    
    protected void show(AbstractNotifyWindow no) {
        if (keyHandler.isEnabled()) {
            BubbleNotify.getInstance().show(no);
        } else {
            no.dispose();
        }
    }
    
}
