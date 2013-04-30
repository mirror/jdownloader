package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.gui.views.SelectionInfo;

public class CleanupSubMenu extends MenuContainer {
    public CleanupSubMenu() {
        setName(org.jdownloader.extensions.extraction.translate.T._.context_cleanup());
        setIconKey("clear");
    }

    @Override
    public JComponent addTo(JComponent root, final SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final JComponent ret = super.addTo(root, selection);
        ret.setEnabled(false);
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                List<Archive> archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) selection).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            ret.setEnabled(true);
                        }
                    };
                }

            }

        });
        return ret;
    }

}
