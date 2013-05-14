package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ExtensionQuickToggleAction extends AppAction implements GenericConfigEventListener<Boolean> {
    private LazyExtension extension;

    public ExtensionQuickToggleAction(SelectionInfo<?, ?> selection, String data) {
        extension = ExtensionController.getInstance().getExtension(data);
        if (extension != null) {
            setName(extension.getName());
            setIconKey(extension.getIconPath());
            if (extension._getExtension() != null) {
                setIconKey(extension._getExtension().getIconKey());

            }
            setSelected(extension._isEnabled());
            BooleanKeyHandler keyHandler = extension._getSettings().getStorageHandler().getKeyHandler(ExtensionConfigInterface.KEY_ENABLED, BooleanKeyHandler.class);
            keyHandler.getEventSender().addListener(this, true);
        }
    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(_GUI._.ExtensionQuickToggleAction_name_selected(extension.getName()));
            setTooltipText(_GUI._.ExtensionQuickToggleAction_name_selected_tt(extension.getName()));

        } else {
            setTooltipText(_GUI._.ExtensionQuickToggleAction_name_deselected_tt(extension.getName()));
            setName(_GUI._.ExtensionQuickToggleAction_name_deselected(extension.getName()));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ExtensionController.getInstance().setEnabled(extension, !extension._isEnabled());

        setSelected(extension._isEnabled());
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        setSelected(newValue);
    }

}
