package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.SelectionInfo;

public class CleanupAutoDeleteLinksEnabledToggleAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtractionAction<PackageType, ChildrenType> {

    public CleanupAutoDeleteLinksEnabledToggleAction(final SelectionInfo<PackageType, ChildrenType> selection) {
        super(selection);
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

        for (Archive archive : archives) {
            archive.getSettings().setRemoveDownloadLinksAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? org.jdownloader.extensions.extraction.translate.T._.set_autoremovelinks_true() : org.jdownloader.extensions.extraction.translate.T._.set_autoremovelinks_false());

    }
}
