package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;

public class ToggleAppAction extends AppAction implements CachableInterface, GenericConfigEventListener<Boolean> {

    private BooleanKeyHandler handler;

    public ToggleAppAction(BooleanKeyHandler handler, String name, String tt) {
        this.handler = handler;

        setName(name);
        setTooltipText(tt);
        setSelected(handler.isEnabled());
        handler.getEventSender().addListener(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handler.toggle();
    }

    @Override
    public void setData(String data) {
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setSelected(handler.isEnabled());
            }
        };
    }

}
