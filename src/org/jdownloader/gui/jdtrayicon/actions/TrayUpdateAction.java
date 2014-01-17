package org.jdownloader.gui.jdtrayicon.actions;

import javax.swing.Icon;

import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class TrayUpdateAction extends UpdateAction implements UpdaterListener {

    public TrayUpdateAction() {
        super();
        updateName();

        UpdateController.getInstance().getEventSender().addListener(this, true);
    }

    private void updateName() {
        if (UpdateController.getInstance().hasPendingUpdates()) {
            setName(_TRAY._.popup_update_install());
            setTooltipText(_TRAY._.popup_update_install_tt());
        } else {
            setTooltipText(createTooltip());
            setName(_TRAY._.popup_update());
        }
    }

    @Override
    public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
        updateName();
    }

    @Override
    public void onUpdaterStatusUpdate(String label, Icon icon, double progress) {
    }

}
