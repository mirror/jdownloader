package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class CleanupSubMenu extends MenuContainer {
    public CleanupSubMenu() {
        setName(org.jdownloader.extensions.extraction.translate.T._.context_cleanup());
        setIconKey("clear");
    }

    @Override
    public JComponent addTo(JComponent root) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        final JComponent ret = super.addTo(root);
        ret.setEnabled(false);
        Thread thread = new Thread() {
            public void run() {
                List<Archive> archives = ArchiveValidator.validate(DownloadsTable.getInstance().getSelectionInfo(true, true)).getArchives();
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
