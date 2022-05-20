package org.jdownloader.controlling.contextmenu;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.gui.translate._GUI;

public class SeparatorData extends MenuItemData {
    public SeparatorData() {
        setName(_GUI.T.SeparatorData_SeparatorData());
    }

    @Override
    public JComponent createItem(MenuBuilder menuBuilder) {
        return new JSeparator();
    }

    public String _getIdentifier() {
        return getClass().getName();
    }
}
