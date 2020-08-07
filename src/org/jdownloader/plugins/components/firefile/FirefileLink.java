package org.jdownloader.plugins.components.firefile;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class FirefileLink {
    public static FirefileLink get(final DownloadLink downloadLink) throws PluginException {
        final Regex matches = new Regex(downloadLink.getDownloadURL(), "https?://firefile\\.cc/drive/s/([a-zA-Z0-9]+)!([a-zA-Z0-9]+)");
        if (matches.getMatch(0) == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (matches.getMatch(1) == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return new FirefileLink(downloadLink, matches.getMatch(0), matches.getMatch(1));
    }

    private FirefileLink(final DownloadLink link, String hash, String key) {
        this.link = link;
        this.hash = hash;
        this.key = key;
    }

    private final DownloadLink link;
    private final String       hash;
    private final String       key;

    public String getHash() {
        return hash;
    }

    public DownloadLink getLink() {
        return link;
    }

    public String getKey() {
        return key;
    }
}
