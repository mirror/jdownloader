package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;

import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;

@ScriptAPI(description = "The context download list link")
public class DownloadLinkSandBox {

    private DownloadLink              downloadLink;
    private DownloadLinkAPIStorableV2 storable;

    public DownloadLinkSandBox(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
        storable = org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl.toStorable(LinkQueryStorable.FULL, downloadLink, this);
    }

    public DownloadLinkSandBox() {
        storable = new DownloadLinkAPIStorableV2();

    }

    public ArchiveSandbox getArchive() {
        if (downloadLink == null || ArchiveValidator.EXTENSION == null) {
            return null;
        }
        Archive archive = ArchiveValidator.EXTENSION.getArchiveByFactory(new DownloadLinkArchiveFactory(downloadLink));
        if (archive != null) {
            return new ArchiveSandbox(archive);
        }
        ArrayList<Object> list = new ArrayList<Object>();
        list.add(downloadLink);
        List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(list);

        return (archives == null || archives.size() == 0) ? null : new ArchiveSandbox(archives.get(0));
    }

    public String getComment() {
        if (downloadLink == null) {
            return null;
        }

        return downloadLink.getComment();
    }

    public String getDownloadPath() {
        if (downloadLink == null) {
            return null;
        }

        return downloadLink.getFileOutput();
    }

    @ScriptAPI(description = "Sets a new filename", parameters = { "new Name" })
    public void setName(String name) {
        if (downloadLink == null) {
            return;
        }
        DownloadWatchDog.getInstance().renameLink(downloadLink, name);

    }

    public String getUrl() {
        if (downloadLink == null) {
            return null;
        }
        return downloadLink.getView().getDisplayUrl();
    }

    public long getBytesLoaded() {
        if (downloadLink == null) {
            return -1;
        }
        return downloadLink.getView().getBytesLoaded();
    }

    public long getBytesTotal() {
        if (downloadLink == null) {
            return -1;
        }
        return downloadLink.getView().getBytesTotal();
    }

    public String getName() {
        if (downloadLink == null) {
            return "ExampleDownloadLink.rar";
        }
        return downloadLink.getName();
    }

    public FilePackageSandBox getPackage() {
        if (downloadLink == null) {
            return new FilePackageSandBox();
        }
        return new FilePackageSandBox(downloadLink.getParentNode());

    }

    public long getSpeed() {

        return storable.getSpeed();

    }

    public String getStatus() {

        return storable.getStatus();

    }

    public String getHost() {

        return storable.getHost();

    }

    public boolean isSkipped() {

        return storable.isSkipped();

    }

    public boolean isRunning() {

        return storable.isRunning();

    }

    public boolean isEnabled() {

        return storable.isEnabled();

    }

    public boolean isFinished() {

        return storable.isFinished();

    }

    public String getExtractionStatus() {
        if (downloadLink == null) {
            return null;
        }
        final ExtractionStatus ret = downloadLink.getExtractionStatus();
        return ret == null ? null : ret.name();
    }

}
