package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveIncompleteArchives extends SelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = 2816227528827363428L;

    public RemoveIncompleteArchives(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setIconKey("unpack");

    }

    public void actionPerformed(ActionEvent e) {

        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {

                try {
                    for (Archive a : new ValidateArchiveAction<CrawledPackage, CrawledLink>((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension(), getSelection()).getArchives()) {
                        final DummyArchive da = a.createDummyArchive();
                        if (!da.isComplete()) {
                            try {
                                Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.RemoveIncompleteArchives_run_(da.getName()), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                List<CrawledLink> l = new ArrayList<CrawledLink>();
                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        l.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                                LinkCollector.getInstance().removeChildren(l);
                            } catch (DialogCanceledException e) {
                                // next archive
                            }
                        }

                    }

                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    Log.exception(e);
                }

            }

        }, true);

    }

}
