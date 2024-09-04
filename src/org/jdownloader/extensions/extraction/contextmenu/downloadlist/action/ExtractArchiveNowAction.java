package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.plugins.FinalLinkState;

import jd.plugins.DownloadLink;

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

    public void actionPerformed(final ActionEvent e) {
        final List<Archive> lArchives = getArchives();
        if (lArchives == null || lArchives.size() == 0) {
            return;
        }
        final Thread thread = new Thread() {
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

    @Override
    public boolean isEnabled() {
        final SelectionInfo<?, ?> selection = getSelection();
        if (selection == null || selection.isEmpty()) {
            return false;
        }
        /**
         * Check if at least one selected item is a finished downloa or a while which exists. </br>
         * This is just a very simple check to provide visual feedback (grey-out action on non allowed items). </br>
         * Believe it or not but some people are using this feature to extract files that they've never downloaded via JDownloader.
         */
        for (final Object o : selection.getChildren()) {
            if (!(o instanceof DownloadLink)) {
                continue;
            }
            final DownloadLink dl = (DownloadLink) o;
            if (FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                return true;
            } else if (new File(dl.getFileOutput()).exists()) {
                /* Target file exists so we can extract it. */
                return true;
            }
        }
        return false;
    }
}