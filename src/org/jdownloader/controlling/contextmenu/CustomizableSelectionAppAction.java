package org.jdownloader.controlling.contextmenu;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public abstract class CustomizableSelectionAppAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableAppAction {

    @SuppressWarnings("unchecked")
    protected SelectionInfo<PackageType, ChildrenType> getSelection() {
        final View view = MainTabbedPane.getInstance().getSelectedView();
        if (view instanceof DownloadsView) {
            return (SelectionInfo<PackageType, ChildrenType>) DownloadsTable.getInstance().getSelectionInfo();
        } else if (view instanceof LinkGrabberView) {
            return (SelectionInfo<PackageType, ChildrenType>) LinkGrabberTable.getInstance().getSelectionInfo();
        } else {
            return null;
        }
    }

    protected boolean hasSelection(SelectionInfo<?, ?> selection2) {
        return selection2 != null && !selection2.isEmpty();
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        setEnabled(hasSelection());
    }

    protected boolean hasSelection() {
        final SelectionInfo<?, ?> selectionInfo = getSelection();
        return hasSelection(selectionInfo);
    }

}
