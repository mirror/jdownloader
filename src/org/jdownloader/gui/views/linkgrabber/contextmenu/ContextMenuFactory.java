package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;

public class ContextMenuFactory {

    private LinkGrabberTable table;

    public ContextMenuFactory(LinkGrabberTable linkGrabberTable) {
        this.table = linkGrabberTable;
    }

    public JPopupMenu createPopup(AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent event) {

        boolean isLinkContext = contextObject instanceof CrawledLink;
        boolean isShift = event.isShiftDown();
        boolean isPkgContext = contextObject instanceof CrawledPackage;
        CrawledLink link = isLinkContext ? (CrawledLink) contextObject : null;
        CrawledPackage pkg = isPkgContext ? (CrawledPackage) contextObject : null;
        JPopupMenu p = new JPopupMenu();
        JMenu m;

        p.add(new ConfirmAction(isShift, selection).toContextMenuAction());

        if (selection == null || selection.size() == 0) {
            p.add(new AddLinksAction().toContextMenuAction());
            return p;
        } else if (JsonConfig.create(LinkgrabberSettings.class).isContextMenuAddLinksActionAlwaysVisible()) {
            p.add(new AddLinksAction().toContextMenuAction());

        }

        p.add(new JSeparator());

        p.add(new OpenDownloadFolderAction(contextObject, selection).toContextMenuAction());
        p.add(new SetDownloadFolderAction(contextObject, selection).toContextMenuAction());

        p.add(new PrioritySubMenu(selection));
        p.add(new FileCheckAction(selection).toContextMenuAction());
        p.add(new CreateDLCAction(selection).toContextMenuAction());
        p.add(new ValidateArchiveAction(selection).toContextMenuAction());
        p.add(new SetDownloadPassword(link, selection).toContextMenuAction());

        p.add(new JSeparator());
        p.add(new MergeToPackageAction(selection).toContextMenuAction());
        p.add(new SplitPackagesByHost(contextObject, selection).toContextMenuAction());

        p.add(new JSeparator());
        p.add(new SortAction(contextObject, selection, column).toContextMenuAction());
        if (isLinkContext) {

            p.add(new OpenUrlAction(link).toContextMenuAction());

        }
        p.add(new JSeparator());
        p.add(new EnabledAction(selection).toContextMenuAction());
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
}
