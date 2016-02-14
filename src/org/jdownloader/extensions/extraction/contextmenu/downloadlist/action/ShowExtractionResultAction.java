package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

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
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.JDGui;

public class ShowExtractionResultAction extends AbstractExtractionContextAction implements GenericConfigEventListener<Boolean> {

    /**
     *
     */

    public ShowExtractionResultAction() {
        super();

        setName(org.jdownloader.extensions.extraction.translate.T.T.contextmenu_extract_show_result());
        setIconKey(IconKey.ICON_ABOUT);

        // CFG_EXTRACTION.WRITE_EXTRACTION_LOG_ENABLED.getEventSender().addListener(this, true);
        updateVisibility();
    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        final List<Archive> lArchives = getArchives();
        if (lArchives != null && lArchives.size() > 0) {
            for (Archive a : lArchives) {
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
        final List<Archive> lArchives = getArchives();
        if (lArchives != null) {
            for (Archive archive : lArchives) {
                if (archive.getExtractLogFile().exists()) {
                    try {
                        ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_OK, T.T.showextractlog(archive.getName()), IO.readFileToString(archive.getExtractLogFile()), null, null, _GUI.T.lit_close()) {

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
                        Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e1.getMessage(), e1);
                    }
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