package org.jdownloader.gui.views.downloads.table;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEvent;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.downloads.context.CheckStatusAction;
import org.jdownloader.gui.views.downloads.context.CopyPasswordAction;
import org.jdownloader.gui.views.downloads.context.CopyURLAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.CustomSpeed;
import org.jdownloader.gui.views.downloads.context.DeleteAction;
import org.jdownloader.gui.views.downloads.context.DeleteFromDiskAction;
import org.jdownloader.gui.views.downloads.context.EditLinkOrPackageAction;
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
import org.jdownloader.gui.views.downloads.context.SetPasswordAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.gui.views.downloads.context.WatchAsYouDownloadAction;

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

        popup.add(new StopsignAction(contextObject));
        popup.add(new EnabledAction(selection));
        popup.add(new ForceDownloadAction(links));
        popup.add(new ResumeAction(links));
        popup.add(new ResetAction(links));
        if (contextObject instanceof FilePackage) {
            popup.add(new WatchAsYouDownloadAction(fps));
        }
        popup.add(new JSeparator());
        popup.add(new NewPackageAction(links));
        popup.add(new CheckStatusAction(links));
        popup.add(new CreateDLCAction(links));
        popup.add(new CopyURLAction(selection));
        popup.add(new JSeparator());
        popup.add(new SetPasswordAction(links));
        popup.add(new CopyPasswordAction(links));
        popup.add(new DeleteAction(links));
        popup.add(new DeleteFromDiskAction(links));
        popup.add(new JSeparator());
        if (contextObject instanceof FilePackage) {
            popup.add(new OpenDirectoryAction(new File(((FilePackage) contextObject).getDownloadDirectory())));
            popup.add(new PackageNameAction(fps));
            popup.add(new PackageDirectoryAction(fps));
        } else if (contextObject instanceof DownloadLink) {
            popup.add(new CustomSpeed((DownloadLink) contextObject));
            popup.add(new OpenDirectoryAction(new File(((DownloadLink) contextObject).getFileOutput()).getParentFile()));
            popup.add(new OpenInBrowserAction(links));
            if (CrossSystem.isOpenFileSupported()) {
                popup.add(new OpenFileAction(new File(((DownloadLink) contextObject).getFileOutput())));
            }
        }
        popup.add(new JSeparator());
        popup.add(PrioritySubMenu.createPrioMenu(links));

        MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableContext(downloadsTable, popup, contextObject, selection, column, ev)));

        popup.add(new JSeparator());
        popup.add(new EditLinkOrPackageAction(downloadsTable, contextObject));
        return popup;
    }
}
