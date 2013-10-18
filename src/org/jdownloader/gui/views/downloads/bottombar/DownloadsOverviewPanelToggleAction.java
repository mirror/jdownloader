package org.jdownloader.gui.views.downloads.bottombar;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadsOverviewPanelToggleAction extends AppAction implements CachableInterface {
    public DownloadsOverviewPanelToggleAction() {
        setIconKey("bottombar");
        setName(_GUI._.DownloadsOverviewPanelToggleAction_DownloadsOverviewPanelToggleAction());
        setSelected(CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.isEnabled());
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                setSelected(newValue);

            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {

            }
        }, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.toggle();
    }

    @Override
    public void setData(String data) {
    }
}
