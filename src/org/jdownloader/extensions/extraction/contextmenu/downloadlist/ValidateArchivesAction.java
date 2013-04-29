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
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.ExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ValidateArchivesAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {
    private SelectionInfo<?, ?> si;
    protected List<Archive>     archives;

    public ValidateArchivesAction(SelectionInfo<?, ?> selection) {
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());
        // if (as.length > 1) {
        //
        // } else {
        // setName(T._.ValidateArchiveAction_ValidateArchiveAction(as[0].getName()));
        // }
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 18), NewTheme.I().getImage("ok", 12), 0, 0, 10, 10)));
        // setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("archive", 18), NewTheme.I().getImage("ok", 11), -1, 0, 6,
        // 8)));

        setEnabled(false);

        this.si = selection;
        //
        if (selection == null) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) si).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (archives.size() > 1) {
                                setName(T._.ValidateArchiveAction_ValidateArchiveAction_multi());
                            } else {
                                setName(T._.ValidateArchiveAction_ValidateArchiveAction(archives.get(0).getName()));
                            }
                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            try {
                DummyArchive da = archive.createDummyArchive();

                DummyArchiveDialog d = new DummyArchiveDialog(da);

                try {
                    Dialog.getInstance().showDialog(d);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            } catch (CheckException e1) {
                Dialog.getInstance().showExceptionDialog("Error", "Cannot Check Archive", e1);
            }
        }
    }

}
