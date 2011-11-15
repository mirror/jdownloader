package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.images.NewTheme;

public class ContextMenuFactory {

    private LinkGrabberTable table;

    public ContextMenuFactory(LinkGrabberTable linkGrabberTable) {
        this.table = linkGrabberTable;
    }

    public JPopupMenu createPopup(AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent event) {
        if (selection == null || selection.size() == 0) return null;
        boolean isLinkContext = contextObject instanceof CrawledLink;
        boolean isShift = event.isShiftDown();
        boolean isPkgContext = contextObject instanceof CrawledPackage;
        CrawledLink link = isLinkContext ? (CrawledLink) contextObject : null;
        CrawledPackage pkg = isPkgContext ? (CrawledPackage) contextObject : null;
        JPopupMenu p = new JPopupMenu();
        JMenu m;
        p.add(new ConfirmAction(isShift, selection).toContextMenuAction());
        p.add(new EnabledAction(selection).toContextMenuAction());
        if (isLinkContext) {
            m = new JMenu(_GUI._.ContextMenuFactory_createPopup_link());
            m.setIcon(link.getIcon());
            m.add(new OpenUrlAction(link).toContextMenuAction());
            m.add(new SetDownloadPassword(link, getChildren(selection)).toContextMenuAction());
            p.add(m);
        }
        if (isPkgContext) {
            m = new JMenu(_GUI._.ContextMenuFactory_createPopup_pkg());
            m.setIcon(NewTheme.I().getIcon("package_open", 18));
            m.add(new OpenDownloadFolderAction(contextObject).toContextMenuAction());
            m.add(new SetDownloadFolderAction(contextObject, getPackages(selection)).toContextMenuAction());
            m.add(new SortAction(selection, column).toContextMenuAction());
            p.add(m);
        }
        p.add(new MergeToPackageAction(selection).toContextMenuAction());
        p.add(new FileCheckAction(selection).toContextMenuAction());
        p.add(new CreateDLCAction(selection).toContextMenuAction());
        p.add(new SplitPackagesByHost(getPackages(selection)).toContextMenuAction());
        p.add(new JSeparator());
        /* remove menu */
        m = new JMenu(_GUI._.ContextMenuFactory_createPopup_cleanup());
        m.setIcon(NewTheme.I().getIcon("clear", 18));
        m.add(new RemoveAllAction().toContextMenuAction());
        m.add(new RemoveSelectionAction(table, selection).toContextMenuAction());
        m.add(new RemoveNonSelectedAction(table, selection).toContextMenuAction());
        m.add(new RemoveOfflineAction().toContextMenuAction());
        m.add(new RemoveIncompleteArchives(selection).toContextMenuAction());
        p.add(m);
        return p;
    }

    private ArrayList<CrawledPackage> getPackages(ArrayList<AbstractNode> selection) {
        ArrayList<CrawledPackage> ret = new ArrayList<CrawledPackage>();
        for (AbstractNode a : selection) {
            if (a instanceof CrawledPackage) ret.add((CrawledPackage) a);
        }
        return ret;
    }

    private ArrayList<CrawledLink> getChildren(ArrayList<AbstractNode> selection) {
        ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
        for (AbstractNode a : selection) {
            if (a instanceof CrawledLink) {
                ret.add((CrawledLink) a);
            } else if (a instanceof CrawledPackage) {
                ret.addAll(((CrawledPackage) a).getChildren());
            }
        }
        return ret;
    }
}
