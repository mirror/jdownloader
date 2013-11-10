package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;

public abstract class AbstractExtensionGuiEnableAction<T extends AbstractExtension<?, ?>> extends AbstractExtensionAction<T> implements GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private BooleanKeyHandler keyHandler;

    public AbstractExtensionGuiEnableAction(BooleanKeyHandler guiEnabled) {

        super();
        keyHandler = guiEnabled;
        setSelected(keyHandler.isEnabled());
        keyHandler.getEventSender().addListener(this, true);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean isActive = _getExtension().getGUI().isActive();
        // activate panel
        if (isActive) {
            _getExtension().getGUI().setActive(false);

        } else {
            _getExtension().getGUI().setActive(true);
            // bring panel to front
            _getExtension().getGUI().toFront();
        }
        setSelected(_getExtension().getGUI().isActive());
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

        setSelected(newValue);

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);

    }

}
