package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;

public class CleanupAutoDeleteLinksEnabledToggleAction extends AbstractExtractionContextAction {

    public CleanupAutoDeleteLinksEnabledToggleAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_autodeletelinks());
        setIconKey(IconKey.ICON_LINK);
        setSelected(false);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives != null && archives.size() > 0) setSelected(_getExtension().isRemoveDownloadLinksAfterExtractEnabled(archives.get(0)));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        for (Archive archive : archives) {
            archive.getSettings().setRemoveDownloadLinksAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        if (JDGui.bugme(WarnLevel.NORMAL)) {
            Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? org.jdownloader.extensions.extraction.translate.T._.set_autoremovelinks_true() : org.jdownloader.extensions.extraction.translate.T._.set_autoremovelinks_false());
        }
    }
}
