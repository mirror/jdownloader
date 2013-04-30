package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.SelectionInfo;

public class ExtractArchiveNowAction extends AbstractExtractionAction {

    /**
 * 
 */

    public ExtractArchiveNowAction(final SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_.contextmenu_extract());
        setIconKey(IconKey.ICON_ARCHIVE_RUN);
        setEnabled(false);

    }

    public void actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                for (Archive archive : archives) {
                    if (archive.isComplete()) {
                        _getExtension().addToQueue(archive);
                    } else {
                        Dialog.getInstance().showMessageDialog(_.cannot_extract_incopmplete(archive.getName()));
                    }
                }

            }
        };
        thread.setName("Extract Context: extract");
        thread.setDaemon(true);
        thread.start();
    }

}