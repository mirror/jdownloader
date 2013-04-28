package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class SeperatorData extends MenuItemData {

    public SeperatorData() {
        setName(_GUI._.SeperatorData_SeperatorData());
    }

    @Override
    public JComponent createItem(SelectionInfo<?, ?> selection) {
        return new JSeparator();
    }

    public String _getIdentifier() {
        return getClass().getName();

    }

}
