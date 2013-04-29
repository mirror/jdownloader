package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileFilter;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.ExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class SetExtractToAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {

    protected List<Archive> archives;

    public SetExtractToAction(final SelectionInfo<?, ?> selection) {
        setName(_.contextmenu_extract_to());
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("folder", 18), NewTheme.I().getImage("edit", 12), 0, 0, 10, 10)));

        setEnabled(false);

        //
        if (selection == null) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) selection).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));

                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {

        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return _.plugins_optional_extraction_filefilter_extractto();
            }
        };
        ArchiveFactory dlAF = archives.get(0).getFactory();
        File extractto = _getExtension().getFinalExtractToFolder(archives.get(0));

        while (extractto != null && !extractto.isDirectory()) {
            extractto = extractto.getParentFile();
        }
        try {
            File path = DownloadFolderChooserDialog.open(extractto, false, _.extract_to2());

            if (path == null) return;
            for (Archive archive : archives) {
                archive.getSettings().setExtractPath(path.getAbsolutePath());
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
