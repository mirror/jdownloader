package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.translate._GUI;

public class ExtensionQuickToggleAction extends CustomizableAppAction implements GenericConfigEventListener<Boolean> {
    private LazyExtension extension;

    public ExtensionQuickToggleAction() {

    }

    @Override
    public void setMenuItemData(MenuItemData data) {
        super.setMenuItemData(data);
        setData(data.getActionData().getData());
    }

    public void setData(String data) {
        extension = ExtensionController.getInstance().getExtension(data);
        if (extension != null) {
            setName(extension.getName());
            setIconKey(extension.getIconPath());
            if (extension._getExtension() != null) {
                setIconKey(extension._getExtension().getIconKey());

            }
            setSelected(extension._isEnabled());
            BooleanKeyHandler keyHandler = extension._getSettings()._getStorageHandler().getKeyHandler(ExtensionConfigInterface.KEY_ENABLED, BooleanKeyHandler.class);
            keyHandler.getEventSender().addListener(this, true);
        }
    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(_GUI._.ExtensionQuickToggleAction_name_selected2(extension.getName()));
            setTooltipText(_GUI._.ExtensionQuickToggleAction_name_selected_tt(extension.getName()));

        } else {
            setTooltipText(_GUI._.ExtensionQuickToggleAction_name_deselected_tt(extension.getName()));
            setName(_GUI._.ExtensionQuickToggleAction_name_deselected2(extension.getName()));
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
