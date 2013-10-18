package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberOverviewPanelToggleAction extends AppAction implements CachableInterface, GenericConfigEventListener<Boolean> {

    public LinkgrabberOverviewPanelToggleAction() {
        setIconKey("bottombar");
        setName(_GUI._.LinkgrabberOverviewPanelToggleAction_LinkgrabberOverviewPanelToggleAction());
        CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this, true);
        setSelected(CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.toggle();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setSelected(CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled());
            }
        };

    }

    @Override
    public void setData(String data) {
    }
}
