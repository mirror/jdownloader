package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;

public abstract class AbstractToolbarToggleAction extends AbstractToolbarAction implements GenericConfigEventListener<Boolean> {

    private BooleanKeyHandler keyHandler;

    public AbstractToolbarToggleAction(BooleanKeyHandler keyHandler) {
        this.keyHandler = keyHandler;
    }

    @Override
    final protected void doInit() {
        setSelected(keyHandler.getValue());
        keyHandler.getEventSender().addListener(this, true);

    }

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
