package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ExtractArchiveNowAction extends AbstractExtractionAction {

    /**
 * 
 */

    public ExtractArchiveNowAction(final SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_.contextmenu_extract());
        Image front = NewTheme.I().getImage("media-playback-start", 20, true);
        setSmallIcon(new ImageIcon(ImageProvider.merge(_getExtension().getIcon(18).getImage(), front, 0, 0, 5, 5)));
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