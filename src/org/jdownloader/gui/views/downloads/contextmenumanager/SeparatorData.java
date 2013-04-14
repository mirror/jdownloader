package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import org.jdownloader.gui.views.SelectionInfo;

public class SeparatorData extends MenuItemData {

    @Override
    public JComponent createItem(SelectionInfo<?, ?> selection) {
        return new JSeparator();
    }
}
