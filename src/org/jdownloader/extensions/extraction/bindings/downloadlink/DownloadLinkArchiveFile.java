package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginProgress;

import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionProgress;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DownloadLinkArchiveFile implements ArchiveFile {

    private List<DownloadLink> downloadLinks;
    private String             name;
    private String             filePath;
    private long               size;
    private Archive            archive;

    public DownloadLinkArchiveFile(DownloadLink link) {

        downloadLinks = new ArrayList<DownloadLink>();
        downloadLinks.add(link);

        name = new File(link.getFileOutput()).getName();
        filePath = link.getFileOutput();
        size = link.getDownloadSize();

    }

    public String toString() {
        return filePath + " Complete " + isComplete() + " valid: " + isValid() + " exists: " + exists();
    }

    @Override
    public int hashCode() {
        return downloadLinks.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DownloadLinkArchiveFile)) return false;
        // this equals is used by the build method of ExtractionExtension. If we have one matching link, the archivefile matches as well
        for (DownloadLink dl : ((DownloadLinkArchiveFile) obj).downloadLinks) {
            if (downloadLinks.contains(dl)) return true;

        }

        return false;
    }

    public boolean isComplete() {
        for (DownloadLink downloadLink : downloadLinks) {
            if (isLinkComplete(downloadLink)) return true;
        }
        return false;
    }

    private boolean isLinkComplete(DownloadLink downloadLink) {
        if (new File(downloadLink.getFileOutput()).exists()) { return true; }
        if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && !downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return false;

        return true;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isValid() {
        for (DownloadLink downloadLink : downloadLinks) {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) return true;
        }
        return false;
    }

    public boolean deleteFile() {
        boolean ret = false;
        for (DownloadLink l : downloadLinks) {
            ret = ret || FileCreationManager.getInstance().delete(new File(l.getFileOutput()));

        }
        return ret;
    }

    public boolean exists() {
        // Log.L.info("Does file exist: " + getName());
        for (DownloadLink l : downloadLinks) {
            if (new File(l.getFileOutput()).exists()) return true;
        }
        return false;
    }

    public List<DownloadLink> getDownloadLinks() {
        return downloadLinks;

    }

    public String getName() {
        return name;
    }

    public void setStatus(ExtractionStatus error) {
        for (DownloadLink downloadLink : downloadLinks) {

            switch (error) {
            case ERRROR_FILE_NOT_FOUND:
                downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                downloadLink.getLinkStatus().setErrorMessage(T._.plugins_optional_extraction_filenotfound());

                break;
            case ERROR_NOT_ENOUGH_SPACE:

                downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_POST_PROCESS);
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

                downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_POST_PROCESS);
                break;
            case IDLE:
                break;
            case RUNNING:
                break;
            case SUCCESSFUL:

                downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
                downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
                downloadLink.getLinkStatus().setStatusText(T._.plugins_optional_extraction_status_extractok());
                downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);

                break;
            }
            downloadLink.setExtractionStatus(error);
        }
    }

    public void setMessage(String text) {
        for (DownloadLink downloadLink : downloadLinks) {
            downloadLink.getLinkStatus().setStatusText(text);

        }
    }

    public void setProgress(long value, long max, Color color) {
        for (DownloadLink downloadLink : downloadLinks) {
            if (value <= 0 && max <= 0) {
                downloadLink.setPluginProgress(null);
            } else {
                PluginProgress progress = downloadLink.getPluginProgress();
                if (progress != null) {
                    progress.updateValues(value, max);
                    progress.setCurrent(value);
                } else {
                    progress = new ExtractionProgress(value, max, color);
                    progress.setIcon(NewTheme.I().getIcon("unpack", 16));
                    progress.setProgressSource(this);
                    downloadLink.setPluginProgress(progress);
                }

            }
        }

    }

    @Override
    public long getFileSize() {
        return size;
    }

    @Override
    public void deleteLink() {
        java.util.List<DownloadLink> list = new ArrayList<DownloadLink>(downloadLinks);
        DownloadController.getInstance().removeChildren(list);
    }

    public void addMirror(DownloadLink link) {
        downloadLinks.add(link);
        size = Math.max(link.getDownloadSize(), size);

    }

    public void setProperty(String key, Object value) {
        for (DownloadLink downloadLink : downloadLinks) {
            downloadLink.setProperty(key, value);
        }
    }

    public Object getProperty(String key) {
        for (DownloadLink downloadLink : downloadLinks) {
            if (downloadLink.hasProperty(key)) { return downloadLink.getProperty(key); }
        }
        return null;
    }

    public AvailableStatus getAvailableStatus() {
        for (DownloadLink downloadLink : downloadLinks) {
            switch (downloadLink.getAvailableStatus()) {
            case TRUE:
                return downloadLink.getAvailableStatus();
            }
        }
        for (DownloadLink downloadLink : downloadLinks) {
            switch (downloadLink.getAvailableStatus()) {
            case UNCHECKED:
                return downloadLink.getAvailableStatus();
            }
        }
        for (DownloadLink downloadLink : downloadLinks) {
            switch (downloadLink.getAvailableStatus()) {
            case UNCHECKABLE:
                return downloadLink.getAvailableStatus();
            }
        }
        return downloadLinks.get(0).getAvailableStatus();
    }

    @Override
    public void onCleanedUp(ExtractionController controller) {
        for (DownloadLink downloadLink : downloadLinks) {
            switch (CFG_GENERAL.CFG.getCleanupAfterDownloadAction()) {
            //
            case CLEANUP_IMMEDIATELY:
                LogController.GL.info("Remove Link " + downloadLink.getName() + " because Finished and CleanupImmediately and Extrating finished!");
                java.util.List<DownloadLink> remove = new ArrayList<DownloadLink>();
                remove.add(downloadLink);
                if (DownloadController.getInstance().askForRemoveVetos(remove)) {
                    DownloadController.getInstance().removeChildren(remove);
                }

                break;

            case CLEANUP_AFTER_PACKAGE_HAS_FINISHED:
                LogController.GL.info("Remove Package " + downloadLink.getName() + " because Finished and CleanupImmediately and Extrating finished!");
                FilePackage fp = downloadLink.getFilePackage();
                if (fp.getControlledBy() != null) {
                    DownloadController.removePackageIfFinished(fp);
                } else {
                    LogController.GL.info("Cannot remove. Package has no controller");
                }
                break;
            case CLEANUP_ONCE_AT_STARTUP:
            case NEVER:
                LogController.GL.info(CFG_GENERAL.CFG.getCleanupAfterDownloadAction() + "");

            }
        }
    }

    @Override
    public void setArchive(Archive archive) {
        this.archive = archive;

        if (archive != null && archive.getFactory() != null) {
            for (DownloadLink downloadLink : downloadLinks) {
                downloadLink.setArchiveID(archive.getFactory().getID());
            }
        }

    }

    public Archive getArchive() {
        return archive;
    }

}
