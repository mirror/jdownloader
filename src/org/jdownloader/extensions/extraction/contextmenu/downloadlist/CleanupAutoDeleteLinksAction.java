package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.gui.views.SelectionInfo;

public class CleanupAutoDeleteLinksAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {

    protected List<Archive> archives;

    public CleanupAutoDeleteLinksAction(final SelectionInfo<?, ?> selection) {
        setName(_.contextmenu_autodeletelinks());

        setIconKey("link");
        setSelected(false);
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

                            setSelected(_getExtension().isRemoveDownloadLinksAfterExtractEnabled(archives.get(0)));
                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {

        for (Archive archive : archives) {
            archive.getSettings().setRemoveDownloadLinksAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoremovelinks_true() : _.set_autoremovelinks_false());

    }
}
