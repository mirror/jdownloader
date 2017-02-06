package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.util.List;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;

public class ExtractArchiveNowAction extends AbstractExtractionContextAction {

    /**
     *
     */

    public ExtractArchiveNowAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T.T.contextmenu_extract());
        setIconKey(IconKey.ICON_RUN);
    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
    }

    public void actionPerformed(ActionEvent e) {
        final List<Archive> lArchives = getArchives();
        if (lArchives != null && lArchives.size() > 0) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    for (Archive archive : lArchives) {
                        if (_getExtension().isComplete(archive)) {
                            _getExtension().addToQueue(archive, true);
                        } else {
                            Dialog.getInstance().showMessageDialog(org.jdownloader.extensions.extraction.translate.T.T.cannot_extract_incomplete(archive.getName()));
                        }
                    }

                }
            };
            thread.setName("Extract Context: extract");
            thread.setDaemon(true);
            thread.start();
        }
    }

}