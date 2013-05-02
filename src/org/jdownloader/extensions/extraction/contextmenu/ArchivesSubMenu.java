package org.jdownloader.extensions.extraction.contextmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.controlling.IOEQ;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchivesSubMenu extends MenuContainer {
    public ArchivesSubMenu() {
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_main());
        setIconKey("archive");
    }

    @Override
    public JComponent addTo(JComponent root, final SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        final JComponent ret = super.addTo(root, selection);
        ret.setEnabled(false);
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                List<Archive> archives = ArchiveValidator.validate((SelectionInfo<?, ?>) selection).getArchives();
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
