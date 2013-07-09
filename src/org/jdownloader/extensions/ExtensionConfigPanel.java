package org.jdownloader.extensions;

import java.awt.Component;

import javax.swing.ImageIcon;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEventListener;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;

public abstract class ExtensionConfigPanel<T extends AbstractExtension> extends AbstractConfigPanel implements ConfigEventListener {

    private static final long serialVersionUID = 1L;

    protected T               extension;

    private BooleanKeyHandler keyHandlerEnabled;

    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    public ExtensionConfigPanel(T plg, boolean clean) {
        super();
        this.extension = plg;
        keyHandlerEnabled = plg.getSettings()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class);

        plg.getSettings()._getStorageHandler().getEventSender().addListener(this);
        if (!clean) {
            final Header header = new Header(plg.getName(), NewTheme.I().getIcon(extension.getIconKey(), 32), keyHandlerEnabled, extension.getVersion());

            add(header, "spanx,growx,pushx");

            header.setEnabled(plg.isEnabled());
            if (plg.getDescription() != null) {
                addDescription(plg.getDescription());
            }
            keyHandlerEnabled.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                    try {
                        extension.setEnabled(header.isHeaderEnabled());
                        updateHeaders(header.isHeaderEnabled());
                    } catch (Exception e1) {
                        Log.exception(e1);
                        Dialog.getInstance().showExceptionDialog("Error", e1.getMessage(), e1);
                    }
                }

                public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                }
            });
        }

    }

    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {

    }

    public ExtensionConfigPanel(T plg) {
        this(plg, false);

    }

    @Override
    protected void onShow() {
        super.onShow();
        updateHeaders(extension.isEnabled());
    }

    private void updateHeaders(boolean b) {
        for (Component c : this.getComponents()) {
            if (c instanceof Header) {
                ((Header) c).setHeaderEnabled(b);
            }
        }
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon(extension.getIconKey(), 32);
    }

    @Override
    public String getTitle() {
        return extension.getName();
    }

    public T getExtension() {
        return extension;
    }
}
