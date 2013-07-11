package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractExtensionQuickToggleAction<T extends AbstractExtension<?, ?>> extends AbstractExtensionAction<T, FilePackage, DownloadLink> implements GenericConfigEventListener<Boolean> {

    private ImageIcon         icon16Enabled;
    private ImageIcon         icon16Disabled;
    private BooleanKeyHandler keyHandler;

    public AbstractExtensionQuickToggleAction(BooleanKeyHandler guiEnabled) {

        super(null);
        keyHandler = guiEnabled;
        setSelected(keyHandler.isEnabled());
        keyHandler.getEventSender().addListener(this, true);
    }

    @Override
    public SelectionInfo<FilePackage, DownloadLink> getSelection() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return super.isSuperEnabled();
    }

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {

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
