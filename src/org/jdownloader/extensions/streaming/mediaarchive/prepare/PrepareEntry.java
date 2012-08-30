package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.util.List;

import jd.plugins.DownloadLink;

public interface PrepareEntry {

    public List<DownloadLink> getLinks();

    public String getName();

}
