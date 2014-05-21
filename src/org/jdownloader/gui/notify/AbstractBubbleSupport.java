package org.jdownloader.gui.notify;

import java.util.List;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfig.BubbleNotifyEnabledState;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;

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
    
    protected void show(final AbstractNotifyWindow no) {
        if (no != null) {
            if (isEnabled()) {
                BubbleNotify.getInstance().show(no);
            } else {
                new EDTRunner() {
                    
                    @Override
                    protected void runInEDT() {
                        no.dispose();
                    }
                };
            }
        }
    }
    
    public boolean isEnabled() {
        return keyHandler.isEnabled() && !BubbleNotifyEnabledState.NEVER.equals(CFG_BUBBLE.CFG.getBubbleNotifyEnabledState());
    }
    
    protected void show(final AbstractNotifyWindowFactory factory) {
        if (isEnabled() && factory != null) {
            BubbleNotify.getInstance().show(factory);
        }
    }
    
}
