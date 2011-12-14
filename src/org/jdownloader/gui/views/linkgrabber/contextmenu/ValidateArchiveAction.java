package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionController;
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
        setEnabled(false);
        extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        for (CrawledLink l : this.selection) {

            // if (extractor.isFilenameSupported(l.getName())) {
            // IExtraction ext = extractor.getExtractorByFilename(l.getName());
            // // ext.buildArchive(null);
            //
            // setEnabled(true);
            // break;
            // }
        }

    }

    public void actionPerformed(ActionEvent e) {
        for (CrawledLink l : this.selection) {

            // if (extractor.isFilenameSupported(l.getName())) {
            // IExtraction ext = extractor.getExtractorByFilename(l.getName());
            // // ext.buildArchive(null);
            // Matcher archiveName = ext.getMatcher();
            // Archive archive = ext.System.out.println(1);
            // }
        }

    }

}
