package jd.controlling.downloadcontroller;

import java.util.HashMap;

import jd.crypt.JDCrypt;
import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;
import org.appwork.utils.encoding.Base64;

public class DownloadLinkStorable implements Storable {

    private static final byte[] KEY     = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    private static final String CRYPTED = "CRYPTED:";
    private DownloadLink        link;

    @SuppressWarnings("unused")
    private DownloadLinkStorable(/* Storable */) {
        this.link = new DownloadLink(null, null, null, null, false);
    }

    public DownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }

    public String getName() {
        return link.getName();
    }

    public void setName(String name) {
        this.link.setName(name);
    }

    public HashMap<String, Object> getProperties() {
        return link.getProperties();
    }

    public void setProperties(HashMap<String, Object> props) {
        this.link.setProperties(props);
    }

    public long getSize() {
        return link.getDownloadSize();
    }

    public void setSize(long size) {
        link.setDownloadSize(size);
    }

    public long getCurrent() {
        return link.getDownloadCurrent();
    }

    public void setCurrent(long current) {
        link.setDownloadCurrent(current);
    }

    public String getURL() {
        if (link.gotBrowserUrl()) {
            byte[] crypted = JDCrypt.encrypt(link.getDownloadURL(), KEY);
            return CRYPTED + Base64.encodeToString(crypted, false);
        } else {
            return link.getDownloadURL();
        }
    }

    public void setURL(String url) {
        if (url.startsWith(CRYPTED)) {
            byte[] bytes = Base64.decodeFast(url.substring(CRYPTED.length()));
            String url2 = JDCrypt.decrypt(bytes, KEY);
            link.setUrlDownload(url2);
        } else {
            link.setUrlDownload(url);
        }
    }

    public String getHost() {
        return link.getHost();
    }

    public void setHost(String host) {
        link.setHost(host);
    }

    public String getBrowserURL() {
        if (!link.gotBrowserUrl()) return null;
        return link.getBrowserUrl();
    }

    public void setBrowserURL(String url) {
        link.setBrowserUrl(url);
    }

    public long[] getChunkProgress() {
        return link.getChunksProgress();
    }

    public void setChunkProgress(long[] p) {
        link.setChunksProgress(p);
    }

    public int getLinkType() {
        return link.getLinkType();
    }

    public void setLinkType(int type) {
        link.setLinkType(type);
    }

    public boolean isEnabled() {
        return link.isEnabled();
    }

    public void setEnabled(boolean b) {
        link.setEnabled(b);
    }

    public long getCreated() {
        return link.getCreated();
    }

    public void setCreated(long time) {
        link.setCreated(time);
    }

    public long getFinished() {
        return link.getFinishedDate();
    }

    public void setFinished(long time) {
        link.setFinishedDate(time);
    }

    /* Do Not Serialize */
    public DownloadLink _getDownloadLink() {
        return link;
    }

}
