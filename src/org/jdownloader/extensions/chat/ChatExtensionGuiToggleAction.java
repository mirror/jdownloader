package org.jdownloader.extensions.chat;

import java.awt.event.ActionEvent;

import org.jdownloader.extensions.AbstractExtensionGuiEnableAction;

public class ChatExtensionGuiToggleAction extends AbstractExtensionGuiEnableAction<ChatExtension> {

    public ChatExtensionGuiToggleAction() {
        super(CFG_CHAT.GUI_ENABLED);

        setIconKey(_getExtension().getIconKey());
        setSelected(_getExtension().getGUI().isActive());
    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(_getExtension().getTranslation().ChatExtensionGuiToggleAction_selected());
            setTooltipText(_getExtension().getTranslation().ChatExtensionGuiToggleAction_selected_tt());
        } else {
            setName(_getExtension().getTranslation().ChatExtensionGuiToggleAction_deselected());
            setTooltipText(_getExtension().getTranslation().ChatExtensionGuiToggleAction_deselected_tt());
        }

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
}
