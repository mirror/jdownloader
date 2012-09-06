package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.InputStream;

import jd.plugins.DownloadLink;

public interface DownloadLinkProvider {

    InputStream getInputStream(DownloadLink dLink, long startPosition, long stopPosition);

    void close();

}
