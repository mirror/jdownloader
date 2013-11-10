package org.jdownloader.controlling.contextmenu;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import org.jdownloader.gui.translate._GUI;

public class SeperatorData extends MenuItemData {

    public SeperatorData() {
        setName(_GUI._.SeperatorData_SeperatorData());
    }

    @Override
    public JComponent createItem() {
        return new JSeparator();
    }

    public String _getIdentifier() {
        return getClass().getName();

    }

}
