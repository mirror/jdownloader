package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;

public abstract class AbstractExtensionQuickToggleAction<T extends AbstractExtension<?, ?>> extends AbstractExtensionAction<T> implements GenericConfigEventListener<Boolean> {

    private BooleanKeyHandler keyHandler;

    public AbstractExtensionQuickToggleAction(BooleanKeyHandler guiEnabled) {

        super();
        keyHandler = guiEnabled;
        setSelected(keyHandler.isEnabled());
        keyHandler.getEventSender().addListener(this, true);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

        setSelected(this.keyHandler.isEnabled());

    }

    public void actionPerformed(ActionEvent e) {

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);

    }

}
