package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.List;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public abstract class AbstractExtractionAction extends AbstractExtensionAction<ExtractionExtension> {

    protected List<Archive> archives;

    public AbstractExtractionAction() {
        super();

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        setEnabled(false);

        if (getSelection() != null && !getSelection().isEmpty()) {
            setVisible(true);
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
        } else {
            setVisible(false);
            setEnabled(false);
        }
    }

    protected void onAsyncInitDone() {
        if (archives != null && archives.size() > 0) {
            super.setEnabled(true);
        } else {
            super.setEnabled(false);
        }

    }

    public void setEnabled(boolean newValue) {

        super.setEnabled(newValue);
    }

    private SelectionInfo<?, ?> getSelection() {
        View view = MainTabbedPane.getInstance().getSelectedView();

        if (view instanceof DownloadsView) {
            return DownloadsTable.getInstance().getSelectionInfo(true, true);

        } else if (view instanceof LinkGrabberView) { return LinkGrabberTable.getInstance().getSelectionInfo(); }
        return null;

    }

    protected void asynchInit() {
        archives = ArchiveValidator.validate(getSelection()).getArchives();
    }

}
