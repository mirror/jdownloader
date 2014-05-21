package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;

public abstract class AbstractToolbarToggleAction extends AbstractToolBarAction implements GenericConfigEventListener<Boolean> {
    
    private BooleanKeyHandler keyHandler;
    
    public AbstractToolbarToggleAction(BooleanKeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        
        keyHandler.getEventSender().addListener(this, true);
        setSelected(keyHandler.getValue());
    }
    
    public void setSelected(final boolean selected) {
        new EDTRunner() {
            
            @Override
            protected void runInEDT() {
                AbstractToolbarToggleAction.super.setSelected(selected);
                if (selected) {
                    setName(getNameWhenEnabled());
                } else {
                    setName(getNameWhenDisabled());
                }
            }
        };
    }
    
    abstract protected String getNameWhenDisabled();
    
    abstract protected String getNameWhenEnabled();
    
    public void actionPerformed(ActionEvent e) {
        boolean sel = isSelected();
        keyHandler.setValue(sel);
    }
    
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }
    
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        setSelected(newValue);
    }
    
}
