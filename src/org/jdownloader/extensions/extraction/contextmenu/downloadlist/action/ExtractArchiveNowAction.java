package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class ExtractArchiveNowAction extends AbstractExtractionContextAction {

    /**
 * 
 */

    public ExtractArchiveNowAction() {
        super();

        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract());

        setSmallIcon(new ExtMergedIcon(new AbstractIcon(org.jdownloader.gui.IconKey.ICON_COMPRESS, 18)).add(new AbstractIcon(IconKey.ICON_MEDIA_PLAYBACK_START, 12), 6, 6));

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();

    }

    public void actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                for (Archive archive : archives) {
                    if (_getExtension().isComplete(archive)) {
                        _getExtension().addToQueue(archive, true);
                    } else {
                        Dialog.getInstance().showMessageDialog(org.jdownloader.extensions.extraction.translate.T._.cannot_extract_incopmplete(archive.getName()));
                    }
                }

            }
        };
        thread.setName("Extract Context: extract");
        thread.setDaemon(true);
        thread.start();
    }

}