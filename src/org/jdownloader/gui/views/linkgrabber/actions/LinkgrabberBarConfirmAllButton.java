package org.jdownloader.gui.views.linkgrabber.actions;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;

public class LinkgrabberBarConfirmAllButton extends ConfirmAllAction implements GenericConfigEventListener<Boolean> {

    public LinkgrabberBarConfirmAllButton() {
        org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().addListener(this, true);
        setAutoStart(org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled());
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (newValue != null) setAutoStart(newValue);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

}
