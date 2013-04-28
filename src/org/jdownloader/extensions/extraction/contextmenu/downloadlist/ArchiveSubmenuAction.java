package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveSubmenuAction extends AppAction {

    public ArchiveSubmenuAction(SelectionInfo<?, ?> selection) {
        setName("Test");
        setIconKey("stop");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

}
