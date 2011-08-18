package jd.controlling.linkcrawler;

import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

public class CrawledLinkInfo {

    private PluginForDecrypt dPlugin = null;

    /**
     * @return the dPlugin
     */
    public PluginForDecrypt getdPlugin() {
        return dPlugin;
    }

    /**
     * @param dPlugin
     *            the dPlugin to set
     */
    public void setdPlugin(PluginForDecrypt dPlugin) {
        this.dPlugin = dPlugin;
    }

    /**
     * @return the hPlugin
     */
    public PluginForHost gethPlugin() {
        return hPlugin;
    }

    /**
     * @param hPlugin
     *            the hPlugin to set
     */
    public void sethPlugin(PluginForHost hPlugin) {
        this.hPlugin = hPlugin;
    }

    private PluginForHost hPlugin = null;
    private DownloadLink  dlLink  = null;

    /**
     * @return the dlLink
     */
    public DownloadLink getDownloadLink() {
        return dlLink;
    }

    /**
     * @return the cLink
     */
    public CryptedLink getCryptedLink() {
        return cLink;
    }

    private CryptedLink cLink = null;
    private String      url;

    public CrawledLinkInfo(DownloadLink dlLink) {
        this.dlLink = dlLink;
    }

    public CrawledLinkInfo(CryptedLink cLink) {
        this.cLink = cLink;
    }

    public CrawledLinkInfo(String url) {
        if (url == null) return;
        this.url = new String(url);
    }

    public String getURL() {
        if (dlLink != null) return dlLink.getDownloadURL();
        if (cLink != null) return cLink.getCryptedUrl();
        if (url != null) return url;
        return null;
    }

    @Override
    public String toString() {
        if (url != null) return "URL:" + getURL();
        if (dlLink != null) return "DLLink:" + getURL();
        if (cLink != null) return "CLink:" + getURL();
        return null;
    }
}
