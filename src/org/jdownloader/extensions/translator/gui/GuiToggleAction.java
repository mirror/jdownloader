package org.jdownloader.extensions.translator.gui;

import org.jdownloader.extensions.AbstractExtensionGuiEnableAction;
import org.jdownloader.extensions.translator.CFG_TRANSLATOR;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.gui.views.SelectionInfo;

public class GuiToggleAction extends AbstractExtensionGuiEnableAction<TranslatorExtension> {

    public GuiToggleAction() {
        super(CFG_TRANSLATOR.GUI_ENABLED);
        setName(_getExtension().getTranslation().Translator());
        setIconKey(_getExtension().getIconKey());
        setSelected(_getExtension().getGUI().isActive());
    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(_getExtension().getTranslation().TranslatorExtensionGuiToggleAction_selected());
            setTooltipText(_getExtension().getTranslation().TranslatorExtensionGuiToggleAction_selected_tt());
        } else {
            setName(_getExtension().getTranslation().TranslatorExtensionGuiToggleAction_deselected());
            setTooltipText(_getExtension().getTranslation().TranslatorExtensionGuiToggleAction_deselected_tt());
        }

    }
}
