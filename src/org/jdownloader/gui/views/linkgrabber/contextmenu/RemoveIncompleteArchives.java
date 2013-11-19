package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.uio.UIOManager;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ExtractIconVariant;
import org.jdownloader.gui.translate._GUI;

public class RemoveIncompleteArchives extends CustomizableSelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = 2816227528827363428L;

    public RemoveIncompleteArchives() {

        setName(_GUI._.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setSmallIcon(new ExtractIconVariant("error", 18));

    }

    public void actionPerformed(ActionEvent e) {

        if (!isEnabled()) return;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    ValidateArchiveAction<CrawledPackage, CrawledLink> va = new ValidateArchiveAction<CrawledPackage, CrawledLink>((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension(), getSelection());
                    for (Archive a : va.getArchives()) {
                        final DummyArchive da = va.createDummyArchive(a);
                        if (!da.isComplete()) {
                            try {
                                if (JDGui.bugme(WarnLevel.LOW)) {
                                    Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.RemoveIncompleteArchives_run_(da.getName()), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                }
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
        };
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName(getClass().getName());
        thread.start();
    }

}
