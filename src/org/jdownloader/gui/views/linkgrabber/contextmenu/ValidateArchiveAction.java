package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

public class ValidateArchiveAction extends AppAction {

    private ArrayList<CrawledLink> selection;
    private ExtractionExtension    extractor;

    public ValidateArchiveAction(ArrayList<AbstractNode> selection) {
        setName(_GUI._.ValidateArchiveAction_ValidateArchiveAction_object_());
        setIconKey("archive");
        this.selection = LinkTreeUtils.getSelectedChildren(selection);
        setEnabled(true);
        extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();

    }

    public void actionPerformed(ActionEvent e) {
        for (CrawledLink l : this.selection) {
            if (extractor.isLinkSupported(l.getName())) {
                Archive archive = extractor.buildArchive(new CrawledLinkFactory(l));
                System.out.println(archive);
                boolean compl = archive.isComplete();
                System.out.println(compl);
            }
        }
    }

}
