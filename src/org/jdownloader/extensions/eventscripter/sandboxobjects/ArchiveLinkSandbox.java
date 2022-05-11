package org.jdownloader.extensions.eventscripter.sandboxobjects;

public class ArchiveLinkSandbox {
    public DownloadLinkSandBox getDownloadLinkSandBox() {
        return downloadLinkSandBox;
    }

    public CrawledLinkSandbox getCrawledLinkSandbox() {
        return crawledLinkSandbox;
    }

    private final CrawledLinkSandbox crawledLinkSandbox;
    private final FilePathSandbox    filePathSandbox;

    public FilePathSandbox getFilePathSandbox() {
        return filePathSandbox;
    }

    private final DownloadLinkSandBox downloadLinkSandBox;
    private final String              missingArchiveLink;

    public String getMissingArchiveLink() {
        return missingArchiveLink;
    }

    public ArchiveLinkSandbox(String missingArchiveLink) {
        this.missingArchiveLink = missingArchiveLink;
        this.filePathSandbox = null;
        this.crawledLinkSandbox = null;
        this.downloadLinkSandBox = null;
    }

    public ArchiveLinkSandbox(DownloadLinkSandBox downloadLinkSandBox) {
        this.downloadLinkSandBox = downloadLinkSandBox;
        this.crawledLinkSandbox = null;
        this.filePathSandbox = null;
        this.missingArchiveLink = null;
    }

    public ArchiveLinkSandbox(CrawledLinkSandbox crawledLinkSandbox) {
        this.crawledLinkSandbox = crawledLinkSandbox;
        this.downloadLinkSandBox = null;
        this.filePathSandbox = null;
        this.missingArchiveLink = null;
    }

    public ArchiveLinkSandbox(FilePathSandbox filePathSandbox) {
        this.filePathSandbox = filePathSandbox;
        this.downloadLinkSandBox = null;
        this.crawledLinkSandbox = null;
        this.missingArchiveLink = null;
    }
}
