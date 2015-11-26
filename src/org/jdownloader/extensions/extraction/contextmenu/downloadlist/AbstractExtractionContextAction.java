package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.List;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator.ArchiveValidation;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public abstract class AbstractExtractionContextAction extends AbstractExtensionAction<ExtractionExtension> {

    private volatile List<Archive> archives = null;

    public List<Archive> getArchives() {
        return archives;
    }

    public AbstractExtractionContextAction() {
        super();
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        requestUpdateSelection();
    }

    protected void requestUpdateSelection() {
        setEnabled(false);
        final SelectionInfo<?, ?> selection = getSelection();
        if (selection != null && !selection.isEmpty()) {
            setVisible(true);
            final ArchiveValidation result = ArchiveValidator.validate(selection, true);
            result.executeWhenReached(new Runnable() {

                @Override
                public void run() {
                    archives = result.getArchives();
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            onAsyncInitDone();
                        }
                    };
                }
            });
        } else {
            setVisible(false);
            setEnabled(false);
        }
    }

    protected void onAsyncInitDone() {
        final List<Archive> lArchives = getArchives();
        if (lArchives != null && lArchives.size() > 0) {
            super.setEnabled(true);
        } else {
            super.setEnabled(false);
        }
    }

    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);
    }

    private SelectionInfo<?, ?> getSelection() {
        final View view = MainTabbedPane.getInstance().getSelectedView();
        if (view instanceof DownloadsView) {
            return DownloadsTable.getInstance().getSelectionInfo();
        } else if (view instanceof LinkGrabberView) {
            return LinkGrabberTable.getInstance().getSelectionInfo();
        }
        return null;
    }

}
