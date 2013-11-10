package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.IOException;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.CFG_EXTRACTION;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class ShowExtractionResultAction extends AbstractExtractionAction implements GenericConfigEventListener<Boolean> {

    /**
 * 
 */

    public ShowExtractionResultAction() {
        super();

        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract_show_result());

        setIconKey(IconKey.ICON_ABOUT);

        // CFG_EXTRACTION.WRITE_EXTRACTION_LOG_ENABLED.getEventSender().addListener(this, true);
        updateVisibility();
    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives != null && archives.size() > 0) {
            for (Archive a : archives) {
                if (a.getExtractLogFile().exists()) {
                    setVisible(true);
                    return;
                }
            }

        }
        setVisible(false);
    }

    private void updateVisibility() {
        setVisible(CFG_EXTRACTION.CFG.isWriteExtractionLogEnabled());
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {

            if (archive.getExtractLogFile().exists()) {
                try {
                    ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_OK, T._.showextractlog(archive.getName()), IO.readFileToString(archive.getExtractLogFile()), null, null, _GUI._.lit_close()) {

                        @Override
                        protected int getPreferredHeight() {
                            return (int) (JDGui.getInstance().getMainFrame().getHeight() * 0.8);
                        }

                        @Override
                        protected int getPreferredWidth() {
                            return (int) (JDGui.getInstance().getMainFrame().getWidth() * 0.8);
                        }

                    };
                    try {
                        Dialog.getInstance().showDialog(d);
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                } catch (IOException e1) {
                    Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
                }
            }

        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        updateVisibility();
    }

}