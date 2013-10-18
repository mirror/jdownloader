package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberSidebarToggleAction extends AppAction implements CachableInterface, GenericConfigEventListener<Boolean> {

    public LinkgrabberSidebarToggleAction() {

        setName(_GUI._.LinkgrabberSidebarToggleAction_LinkgrabberSidebarToggleAction());
        putValue(SMALL_ICON, NewTheme.I().getIcon("sidebar", -1));
        setTooltipText(_GUI._.LinkGrabberPanel_LinkGrabberPanel_btn_showsidebar_tt_up());
        setSelected(CFG_GUI.CFG.isLinkgrabberSidebarVisible());
        CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(this, true);
    }

    public void actionPerformed(ActionEvent e) {
        org.jdownloader.settings.staticreferences.CFG_GUI.CFG.setLinkgrabberSidebarVisible(!org.jdownloader.settings.staticreferences.CFG_GUI.CFG.isLinkgrabberSidebarVisible());
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setSelected(CFG_GUI.CFG.isLinkgrabberSidebarVisible());
            }
        };
    }

    @Override
    public void setData(String data) {
    }

}
