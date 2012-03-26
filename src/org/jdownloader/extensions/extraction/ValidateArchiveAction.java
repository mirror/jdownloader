package org.jdownloader.extensions.extraction;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

public class ValidateArchiveAction extends AppAction {

    private ArrayList<AbstractNode> selection;
    private ExtractionExtension     extractor;
    private ArrayList<Archive>      archives;

    public ValidateArchiveAction(ExtractionExtension extractionExtension, ArrayList<AbstractNode> selection) {
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());
        setIconKey("archive");
        this.selection = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<AbstractNode>());

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
                        Archive archive = extractor.getExtractorByFactory(clf).buildArchive(clf);
                        if (archive != null) {
                            archives.add(archive);
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
                        Archive archive = extractor.getExtractorByFactory(clf).buildArchive(clf);
                        if (archive != null) {
                            archives.add(archive);
                        }
                    }
                }

            }
        }
        setEnabled(archives.size() > 0);
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            boolean compl = archive.isComplete();
            if (compl) {
                Dialog.getInstance().showMessageDialog(T._.ValidateArchiveAction_actionPerformed_(archive.getName(), archive.getArchiveFiles().size()));

            } else {
                Dialog.getInstance().showMessageDialog(T._.ValidateArchiveAction_actionPerformed_bad(archive.getName(), archive.getArchiveFiles().size(), archive.getMissing()));
            }
        }
    }

}
