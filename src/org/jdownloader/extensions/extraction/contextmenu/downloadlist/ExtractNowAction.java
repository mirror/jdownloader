package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ExtractNowAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {
    protected List<Archive> archives;

    /**
 * 
 */

    public ExtractNowAction(final SelectionInfo<?, ?> selection) {
        setName(_.contextmenu_extract());
        Image front = NewTheme.I().getImage("media-playback-start", 20, true);
        setSmallIcon(new ImageIcon(ImageProvider.merge(_getExtension().getIcon(18).getImage(), front, 0, 0, 5, 5)));
        setEnabled(false);
        if (selection == null) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) selection).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                for (Archive archive : archives) {
                    if (archive.isComplete()) {
                        _getExtension().addToQueue(archive);
                    } else {
                        Dialog.getInstance().showMessageDialog(_.cannot_extract_incopmplete(archive.getName()));
                    }
                }

            }
        };
        thread.setName("Extract Context: extract");
        thread.setDaemon(true);
        thread.start();
    }

}