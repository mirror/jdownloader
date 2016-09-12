package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.TaskQueue;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashResult;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;

public class RunCheckSumAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RunCheckSumAction() {
        super();
        setIconKey(org.jdownloader.gui.IconKey.ICON_HASHSUM);
        setName(_GUI.T.gui_table_contextmenu_runchecksum());
    }

    public void actionPerformed(ActionEvent e) {
        for (final DownloadLink downloadLink : getSelection().getChildren()) {
            final Downloadable downloadable = new DownloadLinkDownloadable(downloadLink);
            if (downloadable.isHashCheckEnabled()) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        final File file = new File(downloadable.getFileOutput());
                        if (file.exists()) {
                            final HashInfo hashInfo = downloadable.getHashInfo();
                            final HashResult result = downloadable.getHashResult(hashInfo, file);
                            if (result != null) {
                                if (result.match()) {
                                    final long fileSize = file.length();
                                    downloadLink.setVerifiedFileSize(fileSize);
                                    downloadLink.setDownloadCurrent(fileSize);
                                }
                                downloadLink.setFinalLinkState(result.getFinalLinkState());
                            }
                        }
                        return null;
                    }
                });

            }
        }
    }
}