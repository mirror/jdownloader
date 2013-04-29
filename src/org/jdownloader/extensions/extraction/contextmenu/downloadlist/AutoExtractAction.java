package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

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
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class AutoExtractAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {

    protected List<Archive> archives;

    public AutoExtractAction(final SelectionInfo<?, ?> selection) {
        setName(_.contextmenu_autoextract());
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 18), NewTheme.I().getImage("refresh", 12), 0, 0, 10, 10)));
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
                            setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));

                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            archive.getSettings().setAutoExtract(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoextract_true() : _.set_autoextract_false());
    }

}
