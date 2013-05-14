package org.jdownloader.extensions.infobar;

import java.awt.event.ActionEvent;

import org.jdownloader.extensions.AbstractExtensionGuiEnableAction;
import org.jdownloader.extensions.infobar.translate.T;
import org.jdownloader.gui.views.SelectionInfo;

public class EnableInfoBarGuiAction extends AbstractExtensionGuiEnableAction<InfoBarExtension> {

    public EnableInfoBarGuiAction(SelectionInfo<?, ?> plg) {
        super(CFG_INFOBAR.GUI_ENABLED);
        setName(T._.EnableInfoBarGuiAction());
        setIconKey("info");

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(_getExtension().getTranslation().InfoBarGuiToggleAction_selected());
            setTooltipText(_getExtension().getTranslation().InfoBarGuiToggleAction_selected_tt());
        } else {
            setName(_getExtension().getTranslation().InfoBarGuiToggleAction_deselected());
            setTooltipText(_getExtension().getTranslation().InfoBarGuiToggleAction_deselected_tt());
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        _getExtension().setGuiEnable(isSelected());
    }

}
