package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.TaskQueue;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;

public class AbortExtractionAction extends AbstractExtractionContextAction {

    public AbortExtractionAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_abort());
        setSmallIcon(new ExtractIconVariant("cancel", 18, 14, 0, 0));
    }

    @Override
    protected void onAsyncInitDone() {
        final List<Archive> lArchives = getArchives();
        if (lArchives != null && lArchives.size() > 0) {
            for (final Archive lArchive : lArchives) {
                final ExtractionController extractionController = lArchive.getExtractionController();
                if (extractionController != null && !extractionController.isFinished() && !extractionController.gotKilled()) {
                    setEnabled(true);
                    break;
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        final List<Archive> lArchives = getArchives();
        if (!isEnabled() || lArchives == null) {
            return;
        } else {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (final Archive lArchive : lArchives) {
                        final ExtractionController extractionController = lArchive.getExtractionController();
                        if (extractionController != null && !extractionController.isFinished() && !extractionController.gotKilled()) {
                            _getExtension().cancel(extractionController);
                        }
                    }
                    return null;
                }
            });
        }
    }
}
