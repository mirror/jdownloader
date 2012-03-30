package org.jdownloader.extensions.extraction;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;
import org.jdownloader.images.NewTheme;

public class ValidateArchiveAction extends AppAction {

    private ArrayList<AbstractNode> selection;
    private ExtractionExtension     extractor;
    private ArrayList<Archive>      archives;

    public ValidateArchiveAction(ExtractionExtension extractionExtension, Archive a) {

        setName(T._.ValidateArchiveAction_ValidateArchiveAction(a.getName()));
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("ok", 12), 0, 0, 10, 10)));
        extractor = extractionExtension;
        archives = new ArrayList<Archive>();
        archives.add(a);
        setEnabled(archives.size() > 0);
    }

    public ValidateArchiveAction(ExtractionExtension extractionExtension, ArrayList<AbstractNode> selection) {
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("ok", 12), 0, 0, 10, 10)));
        this.selection = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<AbstractNode>());
        // System.out.println(1);
        extractor = extractionExtension;
        archives = new ArrayList<Archive>();

        nextLink: for (AbstractNode l : this.selection) {
            if (l instanceof CrawledLink) {
                if (((CrawledLink) l).getLinkState() != LinkState.OFFLINE) {
                    CrawledLinkFactory clf = new CrawledLinkFactory(((CrawledLink) l));
                    if (extractor.isLinkSupported(clf)) {

                        for (Archive a : archives) {
                            if (a.contains(clf)) continue nextLink;
                        }
                        try {
                            Archive archive = extractor.getExtractorByFactory(clf).buildArchive(clf);
                            if (archive != null) {
                                archives.add(archive);
                            }
                        } catch (ArchiveException e1) {
                            Log.exception(e1);
                        }
                    }
                }
            } else if (l instanceof DownloadLink) {
                if (((DownloadLink) l).isAvailable() || new File(((DownloadLink) l).getFileOutput()).exists()) {
                    DownloadLinkArchiveFactory clf = new DownloadLinkArchiveFactory(((DownloadLink) l));
                    if (extractor.isLinkSupported(clf)) {

                        for (Archive a : archives) {
                            if (a.contains(clf)) continue nextLink;
                        }
                        try {
                            Archive archive = extractor.getExtractorByFactory(clf).buildArchive(clf);
                            if (archive != null) {
                                archives.add(archive);
                            }
                        } catch (ArchiveException e1) {
                            Log.exception(e1);
                        }
                    }
                }

            }
        }
        setEnabled(archives.size() > 0);
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

    public ArrayList<Archive> getArchives() {
        return archives;
    }

}
