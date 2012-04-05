package org.jdownloader.gui.views.downloads.table;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEvent;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.downloads.context.CheckStatusAction;
import org.jdownloader.gui.views.downloads.context.CopyURLAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.DeleteAction;
import org.jdownloader.gui.views.downloads.context.DeleteFromDiskAction;
import org.jdownloader.gui.views.downloads.context.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.context.NewPackageAction;
import org.jdownloader.gui.views.downloads.context.OpenDirectoryAction;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.gui.views.downloads.context.OpenInBrowserAction;
import org.jdownloader.gui.views.downloads.context.PackageDirectoryAction;
import org.jdownloader.gui.views.downloads.context.PackageNameAction;
import org.jdownloader.gui.views.downloads.context.PrioritySubMenu;
import org.jdownloader.gui.views.downloads.context.ResetAction;
import org.jdownloader.gui.views.downloads.context.ResumeAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.gui.views.downloads.table.linkproperties.URLEditorAction;
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
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
        if (selection != null) {
            for (final AbstractNode node : selection) {
                if (node instanceof DownloadLink) {
                    if (!links.contains(node)) links.add((DownloadLink) node);
                } else {
                    if (!fps.contains(node)) fps.add((FilePackage) node);
                    synchronized (node) {
                        for (final DownloadLink dl : ((FilePackage) node).getChildren()) {
                            if (!links.contains(dl)) {
                                links.add(dl);
                            }
                        }
                    }
                }
            }
        }
        JMenu properties = new JMenu(_GUI._.ContextMenuFactory_createPopup_properties_package());
        popup.add(properties);
        if (contextObject instanceof AbstractPackageNode) {

            Image back = (((AbstractPackageNode<?, ?>) contextObject).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));

        } else if (contextObject instanceof DownloadLink) {

            Image back = (((DownloadLink) contextObject).getIcon().getImage());
            properties.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));

            ((DownloadLink) contextObject).getDefaultPlugin().extendDownloadTablePropertiesMenu(properties, ((DownloadLink) contextObject));

            // package

            // m = new
            // JMenu(_GUI._.ContextMenuFactory_createPopup_properties_package());
            // back = (((DownloadLink)
            // contextObject).getParentNode().isExpanded() ?
            // NewTheme.I().getImage("tree_package_open", 32) :
            // NewTheme.I().getImage("tree_package_closed", 32));
            // m.setIcon(new ImageIcon(ImageProvider.merge(back,
            // NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));
            // popup.add(m);
            properties.add(new SubMenu(_GUI._.ContextMenuFactory_createPopup_speed(), NewTheme.I().getIcon("speed", 20), new SpeedLimitator((DownloadLink) contextObject, links, fps)));
            properties.add(new SubMenu(_GUI._.ContextMenuFactory_createPopup_downloadpassword(), NewTheme.I().getIcon("password", 20), new PasswordEditor((DownloadLink) contextObject, links, fps)));

        }

        // properties.add(new DownloadFolderEditorAction((DownloadLink)
        // contextObject, links, fps));

        properties.add(new URLEditorAction(null, links, fps));
        properties.add(PrioritySubMenu.createPrioMenu(links));
        popup.add(new ForceDownloadAction(links));
        // popup.add(new SuperPriorityDownloadAction(links));
        popup.add(new EnabledAction(selection));
        popup.add(new ResumeAction(links));
        popup.add(new ResetAction(links));
        popup.add(new StopsignAction(contextObject));
        popup.add(new JSeparator());
        popup.add(new NewPackageAction(links));
        popup.add(new CheckStatusAction(links));
        popup.add(new CreateDLCAction(links));
        popup.add(new CopyURLAction(selection));
        popup.add(new JSeparator());

        popup.add(new JSeparator());
        if (contextObject instanceof FilePackage) {
            popup.add(new OpenDirectoryAction(new File(((FilePackage) contextObject).getDownloadDirectory())));
            popup.add(new PackageNameAction(fps));
            popup.add(new PackageDirectoryAction(fps));
        } else if (contextObject instanceof DownloadLink) {

            popup.add(new OpenDirectoryAction(new File(((DownloadLink) contextObject).getFileOutput()).getParentFile()));
            popup.add(new OpenInBrowserAction(links));
            if (CrossSystem.isOpenFileSupported()) {
                popup.add(new OpenFileAction(new File(((DownloadLink) contextObject).getFileOutput())));
            }
        }
        popup.add(new JSeparator());

        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableContext(downloadsTable, popup, contextObject, selection, column, ev)));

        popup.add(new JSeparator());

        /* remove menu */
        JMenu m = new JMenu(_GUI._.ContextMenuFactory_createPopup_cleanup());
        m.setIcon(NewTheme.I().getIcon("clear", 18));
        m.add(new DeleteAction(links));
        m.add(new DeleteFromDiskAction(links));
        popup.add(m);

        // popup.add(new EditLinkOrPackageAction(downloadsTable,
        // contextObject));
        return popup;
    }
}
