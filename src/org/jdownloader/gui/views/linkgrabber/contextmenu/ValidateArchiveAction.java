package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;
import org.jdownloader.translate._JDT;

public class ValidateArchiveAction extends AppAction {

    private ArrayList<CrawledLink> selection;
    private ExtractionExtension    extractor;
    private ArrayList<Archive>     archives;

    public ValidateArchiveAction(ArrayList<AbstractNode> selection) {
        setName(_GUI._.ValidateArchiveAction_ValidateArchiveAction_object_());
        setIconKey("archive");
        this.selection = LinkTreeUtils.getSelectedChildren(selection);

        extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        archives = new ArrayList<Archive>();

        nextLink: for (CrawledLink l : this.selection) {
            if (l.getLinkState() != LinkState.OFFLINE) {
                CrawledLinkFactory clf = new CrawledLinkFactory(l);
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
        setEnabled(archives.size() > 0);
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            System.out.println(archive);
            boolean compl = archive.isComplete();
            System.out.println(compl);
            if (compl) {
                Dialog.getInstance().showMessageDialog(_JDT._.ValidateArchiveAction_actionPerformed_(archive.getName(), archive.getArchiveFiles().size()));

            } else {
                Dialog.getInstance().showMessageDialog(_JDT._.ValidateArchiveAction_actionPerformed_bad(archive.getName(), archive.getArchiveFiles().size(), archive.getMissing()));
            }
        }
    }

}
