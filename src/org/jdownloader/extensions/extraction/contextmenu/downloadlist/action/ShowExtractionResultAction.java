package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.IOException;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.JDGui;

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
import org.jdownloader.gui.views.SelectionInfo;

public class ShowExtractionResultAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtractionAction<PackageType, ChildrenType> {

    /**
 * 
 */

    public ShowExtractionResultAction(final SelectionInfo<PackageType, ChildrenType> selection) {
        super(selection);

        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract_show_result());

        setIconKey(IconKey.ICON_ABOUT);
        setVisible(CFG_EXTRACTION.CFG.isWriteExtractionLogEnabled());
    }

    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        if (CFG_EXTRACTION.CFG.isWriteExtractionLogEnabled()) {
            super.setSelection(selection);
        } else {
            // nothing
            setVisible(false);
        }
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

}