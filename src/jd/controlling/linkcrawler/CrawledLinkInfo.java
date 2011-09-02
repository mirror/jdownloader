package jd.controlling.linkcrawler;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;

public class CrawledLinkInfo implements AbstractPackageChildrenNode<CrawledPackageInfo>, CheckableLink {

    private CrawledPackageInfo parent      = null;
    private PluginForDecrypt   dPlugin     = null;
    private CrawlerJob         sourceJob   = null;
    private long               created     = -1;
    private boolean            isDupeAllow = false;

    /**
     * @return the isDupeAllow
     */
    public boolean isDupeAllow() {
        return isDupeAllow;
    }

    /**
     * @param isDupeAllow
     *            the isDupeAllow to set
     */
    public void setDupeAllow(boolean isDupeAllow) {
        this.isDupeAllow = isDupeAllow;
    }

    /**
     * @return the sourceJob
     */
    public CrawlerJob getSourceJob() {
        return sourceJob;
    }

    /**
     * @param sourceJob
     *            the sourceJob to set
     */
    public void setSourceJob(CrawlerJob sourceJob) {
        this.sourceJob = sourceJob;
    }

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

    public long getSize() {
        if (dlLink != null) return dlLink.getDownloadSize();
        return -1;
    }

    /**
     * @return the hPlugin
     */
    public PluginForHost gethPlugin() {
        if (hPlugin != null) return hPlugin;
        if (dlLink != null) return dlLink.getDefaultPlugin();
        return null;
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

    public String getName() {
        if (dlLink != null) return dlLink.getName();
        return "DUMMY";
    }

    public ImageIcon getHosterIcon(boolean scaled) {
        if (dlLink != null) return dlLink.getHosterIcon(scaled);
        return null;
    }

    public String getHost() {
        if (dlLink != null) return dlLink.getHost();
        return null;
    }

    public ImageIcon getIcon() {
        if (dlLink != null) return dlLink.getIcon();
        return null;
    }

    public int getPriority() {
        if (dlLink != null) return dlLink.getPriority();
        return 0;
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

    public CrawledPackageInfo getParentNode() {
        return parent;
    }

    public void setParentNode(CrawledPackageInfo parent) {
        this.parent = parent;
    }

    public boolean isEnabled() {
        return false;
    }

    public long getCreated() {
        return created;
    }

    protected void setCreated(long created) {
        this.created = created;
    }

    public long getFinishedDate() {
        return 0;
    }
}
