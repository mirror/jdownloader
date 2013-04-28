package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.views.SelectionInfo;

public class CopyOfArchiveSubmenuAction extends AppAction {

    public CopyOfArchiveSubmenuAction(SelectionInfo<?, ?> selection) {
        setName("CopxTest");
        setIconKey("stop");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // ExtractionExtension extension = _getExtension();
        // if (extension == null) return null;
        // extension.onExtendPopupMenu(new DownloadTableContext(root, (SelectionInfo<FilePackage, DownloadLink>) selection,
        // selection.getContextColumn()));
        // return null;
    }

}
