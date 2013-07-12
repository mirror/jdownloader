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

public abstract class AbstractExtensionGuiEnableAction<T extends AbstractExtension<?, ?>> extends AbstractExtensionAction<T, FilePackage, DownloadLink> implements GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ImageIcon         icon16Enabled;
    private ImageIcon         icon16Disabled;
    private BooleanKeyHandler keyHandler;

    @Override
    public SelectionInfo<FilePackage, DownloadLink> getSelection() {
        return null;
    }

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {

    }

    public AbstractExtensionGuiEnableAction(BooleanKeyHandler guiEnabled) {

        super(null);
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
