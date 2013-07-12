package org.jdownloader.extensions.shutdown.actions;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.shutdown.CFG_SHUTDOWN;
import org.jdownloader.extensions.shutdown.ShutdownExtension;
import org.jdownloader.extensions.shutdown.translate.T;
import org.jdownloader.gui.views.SelectionInfo;

public class ShutdownToggleAction extends AbstractExtensionAction<ShutdownExtension, FilePackage, DownloadLink> implements GenericConfigEventListener<Boolean> {

    public ShutdownToggleAction() {
        super(null);
        setIconKey(this._getExtension().getIconKey());
        CFG_SHUTDOWN.SHUTDOWN_ACTIVE.getEventSender().addListener(this, true);
        onConfigValueModified(null, null);
    }

    @Override
    public SelectionInfo<FilePackage, DownloadLink> getSelection() {
        return null;
    }

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (selected) {
            setName(T._.shutdown_toggle_action_enabled());
        } else {
            setName(T._.shutdown_toggle_action_disabled());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CFG_SHUTDOWN.SHUTDOWN_ACTIVE.toggle();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        setSelected(CFG_SHUTDOWN.SHUTDOWN_ACTIVE.isEnabled());
    }

}
