package org.jdownloader.controlling.contextmenu;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import org.jdownloader.gui.translate._GUI;

public class SeparatorData extends MenuItemData {

    public SeparatorData() {
        setName(_GUI._.SeparatorData_SeparatorData());
    }

    @Override
    public JComponent createItem() {
        return new JSeparator();
    }

    public String _getIdentifier() {
        return getClass().getName();

    }

}
