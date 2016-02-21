package org.jdownloader.api.myjdownloader.remotemenu;

import javax.swing.KeyStroke;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class RenameActionLink extends AbstractMyJDSelectionActionLink {
    @Override
    public String getID() {
        return "rename";
    }

    public RenameActionLink() {
        setName(_GUI.T.RenameAction_RenameAction());
        setTooltipText(_GUI.T.RenameAction_RenameAction_tt());
        setIconKey(IconKey.ICON_EDIT);
        setAccelerator(KeyStroke.getKeyStroke("F2"));
    }

}
