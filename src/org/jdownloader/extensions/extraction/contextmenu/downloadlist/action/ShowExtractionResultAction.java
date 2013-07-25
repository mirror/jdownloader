package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.SelectionInfo;

public class ShowExtractionResultAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtractionAction<PackageType, ChildrenType> {

    /**
 * 
 */

    public ShowExtractionResultAction(final SelectionInfo<PackageType, ChildrenType> selection) {
        super(selection);

        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract_show_result());

        setIconKey(IconKey.ICON_ABOUT);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();

    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            if (archive.isComplete()) {

                Dialog.getInstance().showMessageDialog(Dialog.STYLE_LARGE, "Extraction Settings", JSonStorage.toString(archive.getSettings().getExtractionInfo()));
            }
        }
    }

}