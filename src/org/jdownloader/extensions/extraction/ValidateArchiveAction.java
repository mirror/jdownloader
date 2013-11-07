package org.jdownloader.extensions.extraction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ValidateArchiveAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction {

    private ExtractionExtension     extractor;
    private java.util.List<Archive> archives;

    public ValidateArchiveAction(ExtractionExtension extractionExtension, Archive... as) {
        if (as.length > 1) {
            setName(T._.ValidateArchiveAction_ValidateArchiveAction_multi());
        } else {
            setName(T._.ValidateArchiveAction_ValidateArchiveAction(as[0].getName()));
        }
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("ok", 12), 0, 0, 10, 10)));
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

    public ValidateArchiveAction(ExtractionExtension extractionExtension, SelectionInfo<PackageType, ChildrenType> si) {
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("archive", 18), NewTheme.I().getImage("ok", 11), -1, 0, 6, 8)));
        //
        extractor = extractionExtension;
        archives = new ArrayList<Archive>();
        nextLink: for (ChildrenType l : si.getChildren()) {
            if (l instanceof CrawledLink) {
                // if (((CrawledLink) l).getLinkState() != LinkState.OFFLINE) {
                CrawledLinkFactory clf = new CrawledLinkFactory(((CrawledLink) l));
                if (extractor.isLinkSupported(clf)) {

                    for (Archive a : archives) {
                        if (a.contains(clf)) continue nextLink;
                    }

                    Archive archive = extractor.getArchiveByFactory(clf);
                    if (archive != null) {
                        archives.add(archive);
                    }

                    // }
                }
            } else if (l instanceof DownloadLink) {
                // if (((DownloadLink) l).isAvailable() || new File(((DownloadLink) l).getFileOutput()).exists()) {
                DownloadLinkArchiveFactory clf = new DownloadLinkArchiveFactory(((DownloadLink) l));
                if (extractor.isLinkSupported(clf)) {

                    for (Archive a : archives) {
                        if (a.contains(clf)) continue nextLink;
                    }

                    Archive archive = extractor.getArchiveByFactory(clf);
                    if (archive != null) {
                        archives.add(archive);
                    }

                }
                // }

            }
        }
        setEnabled(archives.size() > 0);
    }

    public DummyArchive createDummyArchive(Archive a) throws CheckException {
        return extractor.createDummyArchive(a);
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            try {
                DummyArchive da = createDummyArchive(archive);

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

    public java.util.List<Archive> getArchives() {
        return archives;
    }

}
