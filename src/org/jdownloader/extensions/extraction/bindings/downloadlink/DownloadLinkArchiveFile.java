package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.awt.Color;
import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginProgress;

import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.images.NewTheme;

public class DownloadLinkArchiveFile implements ArchiveFile {

    private DownloadLink downloadLink;

    public DownloadLinkArchiveFile(DownloadLink link) {
        this.downloadLink = link;
    }

    @Override
    public int hashCode() {
        return downloadLink.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DownloadLinkArchiveFile)) return false;
        return downloadLink.equals(((DownloadLinkArchiveFile) obj).downloadLink);
    }

    public boolean isComplete() {
        if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && !downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;
        if (!new File(downloadLink.getFileOutput()).exists()) { return false; }
        return true;
    }

    public String getFilePath() {
        return downloadLink.getFileOutput();
    }

    public boolean isValid() {
        return !downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS);
    }

    public boolean delete() {
        return new File(downloadLink.getFileOutput()).delete();
    }

    public boolean exists() {
        return new File(downloadLink.getFileOutput()).exists();
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public String getName() {
        return new File(downloadLink.getFileOutput()).getName();
    }

    public void setStatus(Status error) {
        switch (error) {
        case ERRROR_FILE_NOT_FOUND:
            downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
            downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
            downloadLink.getLinkStatus().setErrorMessage(T._.plugins_optional_extraction_filenotfound());

            break;
        case ERROR_NOT_ENOUGH_SPACE:

            getDownloadLink().getLinkStatus().setStatus(LinkStatus.ERROR_POST_PROCESS);
            downloadLink.getLinkStatus().setErrorMessage(T._.plugins_optional_extraction_status_notenoughspace());
            // link.setMessage(T._.plugins_optional_extraction_status_notenoughspace());
            break;
        case ERROR_CRC:
            downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
            downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
            downloadLink.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
            downloadLink.getLinkStatus().setErrorMessage(T._.plugins_optional_extraction_crcerrorin(downloadLink.getName()));
            break;
        case ERROR:

            getDownloadLink().getLinkStatus().addStatus(LinkStatus.ERROR_POST_PROCESS);
            break;
        case IDLE:
            break;
        case RUNNING:
            break;
        case SUCCESSFUL:

            getDownloadLink().getLinkStatus().addStatus(LinkStatus.FINISHED);
            getDownloadLink().getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
            getDownloadLink().getLinkStatus().setStatusText(T._.plugins_optional_extraction_status_extractok());
            getDownloadLink().getLinkStatus().setStatus(LinkStatus.FINISHED);
            break;
        }
    }

    public void setMessage(String text) {
        getDownloadLink().getLinkStatus().setStatusText(text);
    }

    public void setProgress(long value, long max, Color color) {
        if (value <= 0 && max <= 0) {
            getDownloadLink().setPluginProgress(null);
        } else {
            PluginProgress progress = getDownloadLink().getPluginProgress();
            if (progress != null) {
                progress.setCurrent(value);
                progress.setColor(color);
                progress.setTotal(max);
            } else {
                progress = new PluginProgress(value, max, color);
                progress.setIcon(NewTheme.I().getIcon("update", 16));
                progress.setProgressSource(this);
                getDownloadLink().setPluginProgress(progress);
            }

        }

    }

}
