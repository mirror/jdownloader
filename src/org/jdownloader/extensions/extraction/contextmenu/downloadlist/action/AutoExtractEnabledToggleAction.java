package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;

public class AutoExtractEnabledToggleAction extends AbstractExtractionContextAction {

    public AutoExtractEnabledToggleAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_autoextract());
        setIconKey(IconKey.ICON_ARCHIVE_REFRESH);
        setSelected(false);

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        if (isSelected()) {
            setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_disable_auto_extract2());
        } else {
            setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_disable_auto_extract2());
        }
    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives != null && archives.size() > 0) setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        for (Archive archive : archives) {
            archive.setAutoExtract(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? org.jdownloader.extensions.extraction.translate.T._.set_autoextract_true() : org.jdownloader.extensions.extraction.translate.T._.set_autoextract_false());
    }

}
