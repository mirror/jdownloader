package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEvent;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.SetCommentAction;
import org.jdownloader.gui.views.downloads.table.linkproperties.URLEditorAction;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
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

        if (selection != null && selection.size() > 0) {
            p.add(new ConfirmAction(isShift, selection).toContextMenuAction());
            p.add(new JSeparator());
        }

        if (selection == null || selection.size() == 0 || JsonConfig.create(LinkgrabberSettings.class).isContextMenuAddLinksActionAlwaysVisible()) {
            p.add(new AddLinksAction().toContextMenuAction());
            p.add(new AddContainerAction().toContextMenuAction());
            if (selection == null || selection.size() == 0) { return p; }
            p.add(new JSeparator());
        }

        JMenu properties = new JMenu(_GUI._.ContextMenuFactory_createPopup_properties_package());
        p.add(properties);
        p.add(new JSeparator());
        if (contextObject instanceof AbstractPackageNode) {
            Image back = (((AbstractPackageNode<?, ?>) contextObject).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));
        } else if (contextObject instanceof CrawledLink) {
            Image back = (((CrawledLink) contextObject).getDownloadLink().getIcon().getImage());
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));
            int count = p.getComponentCount();
            try {
                ((CrawledLink) contextObject).getDownloadLink().getDefaultPlugin().extendLinkgrabberTablePropertiesMenu(properties, ((CrawledLink) contextObject));
            } catch (final Throwable e) {
                Log.exception(e);
            }
            if (p.getComponentCount() > count) p.add(new JSeparator());
        }
        for (JMenuItem mm : fillPropertiesMenu(contextObject, selection, column)) {
            properties.add(mm);
        }
        p.add(new CheckStatusAction(selection).toContextMenuAction());
        p.add(new CreateDLCAction(selection).toContextMenuAction());

        p.add(new JSeparator());
        p.add(new MergeToPackageAction(selection).toContextMenuAction());
        p.add(new SplitPackagesByHost(contextObject, selection).toContextMenuAction());

        p.add(new JSeparator());
        p.add(new SortAction(contextObject, selection, column).toContextMenuAction());
        p.add(new JSeparator());
        if (isLinkContext) {
            p.add(new OpenUrlAction(link).toContextMenuAction());
            p.add(new JSeparator());
        }
        int count = p.getComponentCount();
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new LinkgrabberTableContext(table, p, contextObject, selection, column, event)));
        if (p.getComponentCount() > count) p.add(new JSeparator());

        /* remove menu */
        m = new JMenu(_GUI._.ContextMenuFactory_createPopup_cleanup());
        m.setIcon(NewTheme.I().getIcon("clear", 18));
        m.add(new RemoveAllAction().toContextMenuAction());
        m.add(new RemoveSelectionAction(selection).toContextMenuAction());
        m.add(new RemoveNonSelectedAction(table, selection).toContextMenuAction());
        m.add(new RemoveOfflineAction().toContextMenuAction());
        m.add(new RemoveIncompleteArchives(selection).toContextMenuAction());
        p.add(m);

        return p;
    }

    public static ArrayList<JMenuItem> fillPropertiesMenu(AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column) {
        ArrayList<AbstractNode> inteliSelect = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<AbstractNode>());
        ArrayList<JMenuItem> ret = new ArrayList<JMenuItem>();
        ret.add(new JMenuItem(new EnabledAction(inteliSelect).toContextMenuAction()));
        ret.add(new JMenuItem(new URLEditorAction(null, inteliSelect)));
        ret.add(new JMenuItem(new SetDownloadFolderInLinkgrabberAction(contextObject, inteliSelect).toContextMenuAction()));
        ret.add(new JMenuItem(new SetDownloadPassword(contextObject, inteliSelect).toContextMenuAction()));
        ret.add(new JMenuItem(new SetCommentAction(contextObject, inteliSelect).toContextMenuAction()));
        ret.add(new PrioritySubMenu(selection));
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new LinkgrabberTablePropertiesContext(ret, contextObject, selection, column)));
        return ret;
    }
}
