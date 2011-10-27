package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.images.NewTheme;

public class ContextMenuFactory {

    private LinkGrabberTable table;

    public ContextMenuFactory(LinkGrabberTable linkGrabberTable) {
        table = linkGrabberTable;
    }

    public JPopupMenu createPopup(AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column) {
        boolean isLinkContext = contextObject instanceof CrawledLink;
        boolean isPkgContext = contextObject instanceof CrawledPackage;
        CrawledLink link = isLinkContext ? (CrawledLink) contextObject : null;
        CrawledPackage pkg = isPkgContext ? (CrawledPackage) contextObject : null;
        JPopupMenu p = new JPopupMenu();
        JMenu m;
        p.add(new ConfirmAction(selection));
        if (isLinkContext) {
            p.add(new OpenUrlAction(link));
            m = new JMenu(_GUI._.ContextMenuFactory_createPopup_link());
            m.setIcon(link.getIcon());
            m.add(new EditUrlAction(link).toContextMenuAction());
            m.add(new EditFilenameAction(link).toContextMenuAction());
            p.add(m);
        }
        if (isPkgContext) {
            m = new JMenu(_GUI._.ContextMenuFactory_createPopup_pkg());
            m.setIcon(NewTheme.I().getIcon("package_open", 20));
            m.add(new RenamePackageAction(pkg));
            p.add(m);
        }

        return p;
    }
}
