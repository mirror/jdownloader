package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsPanel;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class RenameAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public RenameAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
        setName(_GUI._.RenameAction_RenameAction());
        setTooltipText(_GUI._.RenameAction_RenameAction_tt());
        setIconKey("edit");
        setAccelerator(KeyStroke.getKeyStroke("F2"));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // LinkgrabberContextMenuManager.getInstance().getPanel().getTable().get
            if (si != null) {
                FileColumn col = si.getContextColumn().getModel().getColumnByClass(FileColumn.class);
                if (col != null) {
                    col.startEditing(si.getRawContext());
                }
            } else {
                View view = MainTabbedPane.getInstance().getSelectedView();

                if (view instanceof DownloadsView) {
                    DownloadsTable table = ((DownloadsPanel) ((DownloadsView) view).getContent()).getTable();
                    FileColumn col = table.getExtTableModel().getColumnByClass(FileColumn.class);
                    AbstractNode obj = table.getExtTableModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex());
                    col.startEditing(obj);
                } else if (view instanceof LinkGrabberView) {
                    LinkGrabberTable table = ((LinkGrabberPanel) ((LinkGrabberView) view).getContent()).getTable();
                    FileColumn col = table.getExtTableModel().getColumnByClass(FileColumn.class);
                    AbstractNode obj = table.getExtTableModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex());
                    col.startEditing(obj);
                }
            }
        } catch (Exception ee) {
            // many casts here.... let's catch everything - just to be sure

        }

    }
}
