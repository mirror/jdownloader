package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;

public class SetExtractToAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtractionAction<PackageType, ChildrenType> {

    public SetExtractToAction(final SelectionInfo<PackageType, ChildrenType> selection) {
        super(selection);
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract_to());
        setIconKey(IconKey.ICON_FOLDER);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        File extractto = _getExtension().getFinalExtractToFolder(archives.get(0));

        while (extractto != null && !extractto.isDirectory()) {
            extractto = extractto.getParentFile();
        }
        try {
            File path = DownloadFolderChooserDialog.open(extractto, false, org.jdownloader.extensions.extraction.translate.T._.extract_to2());
            if (path == null) return;
            for (Archive archive : archives) {
                archive.getSettings().setExtractPath(path.getAbsolutePath());
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
