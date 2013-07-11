package org.jdownloader.gui.views.downloads.action;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.NewTheme;

public class RemoveOptionsAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 7579020566025178078L;
    private JButton           positionComp;
    private DownloadsTable    table;

    public RemoveOptionsAction(DownloadsTable table, JButton addLinks) {
        setSmallIcon(NewTheme.I().getIcon("popupButton", -1));
        setTooltipText(_GUI._.RemoveOptionsAction_RemoveOptionsAction_tt());
        positionComp = addLinks;
        this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        java.util.List<AbstractNode> selection = table.getModel().getSelectedObjects();
        popup.add(new RemoveAllVisibleDownloadsAction());

        JMenu m = new JMenu(_GUI._.RemoveOptionsAction_actionPerformed_selected());
        m.setIcon(NewTheme.I().getIcon("delete", 18));
        SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, selection, null, null, e, DownloadsTableModel.getInstance().getTable());
        m.add(new DeleteSelectedLinks(si));
        m.add(new DeleteDisabledSelectedLinks(si));
        m.add(new DeleteSelectedAndFailedLinksAction(si));
        m.add(new DeleteSelectedFinishedLinksAction(si));
        m.add(new DeleteSelectedOfflineLinksAction(si));
        m.setEnabled(selection.size() > 0);
        popup.add(m);
        popup.add(new RemoveNonSelectedAction(selection));
        popup.add(new RemoveOfflineAction());

        // popup.add(new CleanupDownloads());
        // popup.add(new CleanupPackages());
        // popup.addSeparator();
        // popup.add(new RemoveDupesAction());
        // popup.add(new RemoveDisabledAction());
        // popup.add(new RemoveOfflineAction());
        // popup.add(new RemoveFailedAction());
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);

        popup.show(positionComp, -insets[1] - 1, -popup.getPreferredSize().height + insets[2]);
    }
}
