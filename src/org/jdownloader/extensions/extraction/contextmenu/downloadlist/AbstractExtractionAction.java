package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractExtractionAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtensionAction<ExtractionExtension, PackageType, ChildrenType> {

    protected List<Archive> archives;

    public AbstractExtractionAction(SelectionInfo<PackageType, ChildrenType> selection) {
        super(selection);
    }

    protected void onAsyncInitDone() {
        if (archives != null && archives.size() > 0) {
            super.setEnabled(true);
        } else {
            super.setEnabled(false);
        }

    }

    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        this.selection = selection;
        setEnabled(false);
    }

    public void setEnabled(boolean newValue) {
        if (!newValue && getSelection() != null) {
            Thread thread = new Thread() {
                public void run() {
                    asynchInit();
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {

                            onAsyncInitDone();
                        }
                    };
                };
            };
            thread.setDaemon(true);
            thread.setName("SetEnabled: " + getClass().getName());
            thread.start();
        }
        super.setEnabled(newValue);
    }

    protected void asynchInit() {
        archives = ArchiveValidator.validate(getSelection()).getArchives();
    }

}
