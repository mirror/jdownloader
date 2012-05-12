package org.jdownloader.gui.views.downloads.table;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEvent;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PrioritySubMenu;
import org.jdownloader.gui.views.components.packagetable.context.SetCommentAction;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadPassword;
import org.jdownloader.gui.views.components.packagetable.context.URLEditorAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.context.NewPackageAction;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.gui.views.downloads.context.PackageNameAction;
import org.jdownloader.gui.views.downloads.context.ResetAction;
import org.jdownloader.gui.views.downloads.context.ResumeAction;
import org.jdownloader.gui.views.downloads.context.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SortAction;
import org.jdownloader.images.NewTheme;

public class DownloadTableContextMenuFactory {
    private static final DownloadTableContextMenuFactory INSTANCE = new DownloadTableContextMenuFactory();

    /**
     * get the only existing instance of DownloadTableContextMenuFactory. This
     * is a singleton
     * 
     * @return
     */
    public static DownloadTableContextMenuFactory getInstance() {
        return DownloadTableContextMenuFactory.INSTANCE;
    }

    /**
     * Create a new instance of DownloadTableContextMenuFactory. This is a
     * singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private DownloadTableContextMenuFactory() {

    }

    public JPopupMenu create(DownloadsTable downloadsTable, JPopupMenu popup, AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {
        if (selection == null && contextObject == null) return popup;

        SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(contextObject, selection, ev, null, downloadsTable);

        JMenu properties = new JMenu(_GUI._.ContextMenuFactory_createPopup_properties_package());
        popup.add(properties);
        popup.add(new JSeparator());
        if (si.isPackageContext()) {
            Image back = (si.getPackage().isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));

        } else if (si.isChildContext()) {
            Image back = (si.getChild().getIcon().getImage());
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));
            int count = popup.getComponentCount();
            try {
                si.getChild().getDefaultPlugin().extendDownloadTablePropertiesMenu(properties, ((DownloadLink) contextObject));
            } catch (final Throwable e) {
                Log.exception(e);
            }
            if (popup.getComponentCount() > count) popup.add(new JSeparator());
        }
        for (JMenuItem mm : fillPropertiesMenu(si, column)) {
            properties.add(mm);
        }
        popup.add(new CheckStatusAction(si).toContextMenuAction());
        popup.add(new ForceDownloadAction(si));
        popup.add(new ResumeAction(si));
        popup.add(new ResetAction(si));
        popup.add(new StopsignAction(si));
        popup.add(new JSeparator());
        popup.add(new NewPackageAction(si));
        popup.add(new CreateDLCAction(si));

        popup.add(new JSeparator());
        popup.add(new SortAction(contextObject, selection, column).toContextMenuAction());
        popup.add(new JSeparator());
        if (contextObject instanceof FilePackage) {
            popup.add(new PackageNameAction(si));
            popup.add(new JSeparator());
        } else if (contextObject instanceof DownloadLink) {
            if (CrossSystem.isOpenFileSupported()) {
                popup.add(new OpenFileAction(new File(((DownloadLink) contextObject).getFileOutput())));
                popup.add(new JSeparator());
            }
        }
        int count = popup.getComponentCount();
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableContext(popup, si, column)));
        if (popup.getComponentCount() > count) popup.add(new JSeparator());

        /* remove menu */
        JMenu m = new JMenu(_GUI._.ContextMenuFactory_createPopup_cleanup());
        m.setIcon(NewTheme.I().getIcon("clear", 18));
        m.add(createDeleteFromList(si));
        m.add(createDeleteFromListAndDisk(si));

        popup.add(m);

        return popup;
    }

    private JMenu createDeleteFromListAndDisk(SelectionInfo<FilePackage, DownloadLink> si) {
        // new DeleteFromDiskAction(si)
        JMenu ret = new JMenu(_GUI._.DownloadTableContextMenuFactory_createDeleteFromListAndDisk_object_());
        ret.setIcon(NewTheme.I().getIcon("delete", 18));
        ret.add(new DeleteDisabledLinksFromListAndDiskAction(si));
        ret.add(new DeleteFailedFromListAndDiskAction(si));
        ret.add(new DeleteSuccessFulFromListAndDiskAction(si));
        ret.add(new DeleteOfflineFromListAndDiskAction(si));
        ret.add(new DeleteAllFromListAndDiskAction(si));
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableDeletefromListAndDiskContext(ret, si)));
        return ret;
    }

    private JMenu createDeleteFromList(SelectionInfo<FilePackage, DownloadLink> si) {
        // new DeleteFromDiskAction(si)
        JMenu ret = new JMenu(_GUI._.DownloadTableContextMenuFactory_createDeleteFromList_object_());
        ret.setIcon(NewTheme.I().getIcon("list", 18));
        ret.add(new DeleteFailedAction(si));
        ret.add(new DeleteDisabledLinksAction(si));
        ret.add(new DeleteSuccessFulAction(si));
        ret.add(new DeleteOfflineAction(si));

        ret.add(new DeleteAllAction(si));
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableDeletefromListContext(ret, si)));

        return ret;
    }

    public static ArrayList<JMenuItem> fillPropertiesMenu(SelectionInfo<FilePackage, DownloadLink> si, ExtColumn<AbstractNode> column) {

        ArrayList<JMenuItem> ret = new ArrayList<JMenuItem>();
        ret.add(new JMenuItem(new EnabledAction<FilePackage, DownloadLink>(si)));
        ret.add(new JMenuItem(new URLEditorAction(si)));
        ret.add(new JMenuItem(new SetDownloadFolderInDownloadTableAction(si).toContextMenuAction()));
        ret.add(new JMenuItem(new SetDownloadPassword<FilePackage, DownloadLink>(si).toContextMenuAction()));
        ret.add(new JMenuItem(new SetCommentAction<FilePackage, DownloadLink>(si).toContextMenuAction()));
        // ret.add(new JMenuItem(new SpeedLimitAction(contextObject,
        // inteliSelect)));
        ret.add(new PrioritySubMenu<FilePackage, DownloadLink>(si));
        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTablePropertiesContext(ret, si, column)));
        return ret;
    }
}
