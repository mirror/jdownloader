package org.jdownloader.extensions.extraction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.images.NewTheme;

public class ValidateArchiveAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction {

    private final ExtractionExtension     extractor;
    private final java.util.List<Archive> archives;

    public ValidateArchiveAction(ExtractionExtension extractionExtension, Archive... as) {
        if (as.length > 1) {
            setName(T.T.ValidateArchiveAction_ValidateArchiveAction_multi());
        } else {
            setName(T.T.ValidateArchiveAction_ValidateArchiveAction(as[0].getName()));
        }
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage(org.jdownloader.gui.IconKey.ICON_EXTRACT, 20), NewTheme.I().getImage("ok", 12), 0, 0, 10, 10)));
        extractor = extractionExtension;
        archives = new ArrayList<Archive>();
        for (Archive a : as) {
            archives.add(a);
        }
        setEnabled(archives.size() > 0);
    }

    public ExtractionExtension getExtractionExtension() {
        return extractor;
    }

    public DummyArchive createDummyArchive(Archive a) throws CheckException {
        if (extractor != null) {
            return extractor.createDummyArchive(a);
        } else {
            return null;
        }
    }

    public void actionPerformed(ActionEvent e) {
        new Thread("ValidateArchiveAction") {
            {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    for (Archive archive : archives) {
                        try {
                            final DummyArchive da = createDummyArchive(archive);
                            if (da != null) {
                                final DummyArchiveDialog d = new DummyArchiveDialog(da);
                                try {
                                    Dialog.getInstance().showDialog(d);
                                } catch (DialogCanceledException e1) {
                                }
                            }
                        } catch (CheckException e1) {
                            Dialog.getInstance().showExceptionDialog("Error", "Cannot Check Archive", e1);
                        }
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                }
            }
        }.start();
    }

    public java.util.List<Archive> getArchives() {
        return archives;
    }

}
