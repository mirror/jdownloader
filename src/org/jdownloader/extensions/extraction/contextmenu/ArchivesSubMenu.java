package org.jdownloader.extensions.extraction.contextmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class ArchivesSubMenu extends MenuContainer {
    public ArchivesSubMenu() {
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_main());
        setIconKey(org.jdownloader.gui.IconKey.ICON_COMPRESS);
    }

    @Override
    public JComponent addTo(JComponent root) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        final JComponent ret = super.addTo(root);
        ret.setEnabled(false);
        final SelectionInfo<FilePackage, DownloadLink> sel = DownloadsTable.getInstance().getSelectionInfo(true, true);
        Thread thread = new Thread() {
            public void run() {
                List<Archive> archives = ArchiveValidator.validate(sel).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            ret.setEnabled(true);
                        }
                    };
                }
            };
        };
        thread.setDaemon(true);
        thread.setName("SetEnabled: " + getClass().getName());
        thread.start();
        return ret;
    }

}
