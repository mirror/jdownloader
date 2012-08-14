package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
import org.jdownloader.extensions.streaming.dataprovider.DataProvider;
import org.jdownloader.extensions.streaming.dataprovider.DownloadLinkProvider;
import org.jdownloader.extensions.streaming.dataprovider.LocalFileProvider;

public class ArchiveFileProvider implements DataProvider<ArchiveFile> {

    private DownloadLinkProvider downloadLinkProvider;
    private LocalFileProvider    localFileProvider;

    public ArchiveFileProvider(DownloadLinkProvider downloadLinkProvider) {

        this.downloadLinkProvider = downloadLinkProvider;
        localFileProvider = new LocalFileProvider();
    }

    @Override
    public boolean canHandle(ArchiveFile link, DataProvider<?>... dataProviders) {
        return link instanceof ArchiveFile;
    }

    @Override
    public boolean isRangeRequestSupported(ArchiveFile link) {
        return true;
    }

    @Override
    public long getFinalFileSize(ArchiveFile link) {
        return link.getFileSize();
    }

    @Override
    public InputStream getInputStream(ArchiveFile link, long startPosition, long stopPosition) throws IOException {

        if (link instanceof DownloadLinkArchiveFile) {
            List<DownloadLink> links = ((DownloadLinkArchiveFile) link).getDownloadLinks();
            DownloadLink dLink = links.get((int) (Math.random() * links.size()));
            return downloadLinkProvider.getInputStream(dLink, startPosition, stopPosition);

        } else if (link instanceof FileArchiveFile) {

        return localFileProvider.getInputStream(((FileArchiveFile) link).getFile(), startPosition, stopPosition); }

        throw new IOException("Not SUpported: " + link);
    }

    @Override
    public void close() throws IOException {
        Throwable t = null;
        try {
            downloadLinkProvider.close();
        } catch (Throwable e) {
            t = e;
        }
        try {
            localFileProvider.close();
        } catch (Throwable e) {
            if (t == null) t = e;
        }
        if (t != null) throw new IOException(t);
    }

    @Override
    public Throwable getException() {
        return null;
    }

}
