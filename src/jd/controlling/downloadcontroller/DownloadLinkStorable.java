package jd.controlling.downloadcontroller;

import java.util.HashMap;
import java.util.Map;

import jd.crypt.JDCrypt;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.FinalLinkState;

public class DownloadLinkStorable implements Storable {

    private static final byte[] KEY     = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    private static final String CRYPTED = "CRYPTED:";
    private DownloadLink        link;

    public AvailableStatus getAvailablestatus() {
        return link.getAvailableStatus();
    }

    public void setAvailablestatus(AvailableStatus availablestatus) {
        if (availablestatus != null) {
            link.setAvailableStatus(availablestatus);
        }
    }

    @SuppressWarnings("unused")
    private DownloadLinkStorable(/* Storable */) {
        this.link = new DownloadLink(null, null, null, null, false);
    }

    public DownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }

    public long getUID() {
        return link.getUniqueID().getID();
    }

    /**
     * @since JD2
     */
    public void setUID(long id) {
        if (id != -1) link.getUniqueID().setID(id);
    }

    public String getName() {
        return link.getName();
    }

    public void setName(String name) {
        this.link.setName(name);
    }

    public Map<String, Object> getProperties() {
        if (crypt()) return null;
        return link.getProperties();
    }

    public void setProperties(Map<String, Object> props) {
        if (props == null || props.size() == 0) return;
        this.link.setProperties(props);
    }

    /**
     * keep for compatibility
     * 
     * @return
     */
    @Deprecated
    public HashMap<String, String> getLinkStatus() {
        return null;
    }

    public void setFinalLinkState(String state) {
        if (state != null) {
            try {
                link.setFinalLinkState(FinalLinkState.valueOf(state));
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String getFinalLinkState() {
        FinalLinkState state = link.getFinalLinkState();
        if (state != null) { return state.name(); }
        return null;
    }

    public void setLinkStatus(HashMap<String, String> status) {
        if (status != null) {
            try {
                int linkStatus = Integer.parseInt(status.get("status"));
                String errormsg = status.get("errormsg");
                String statustxt = status.get("statustxt");
                if (linkStatus == LinkStatus.FINISHED || hasStatus(linkStatus, LinkStatus.FINISHED)) {
                    link.setFinalLinkState(FinalLinkState.FINISHED);
                } else if (linkStatus == LinkStatus.ERROR_FILE_NOT_FOUND || hasStatus(linkStatus, LinkStatus.ERROR_FILE_NOT_FOUND)) {
                    link.setFinalLinkState(FinalLinkState.OFFLINE);
                } else if (linkStatus == LinkStatus.ERROR_FATAL || hasStatus(linkStatus, LinkStatus.ERROR_FATAL)) {
                    link.setFinalLinkState(FinalLinkState.FAILED_FATAL);
                }
            } catch (final Throwable e) {
                Log.exception(e);
            }
        }
    }

    private boolean hasStatus(final int is, final int expected) {
        return (is & expected) != 0;
    }

    public long getSize() {
        return link.getKnownDownloadSize();
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
        if (crypt()) {
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

    /* Do Not Serialize */
    public DownloadLink _getDownloadLink() {
        return link;
    }

    private boolean crypt() {
        return link.gotBrowserUrl() || DownloadLink.LINKTYPE_CONTAINER == link.getLinkType();
    }

    /**
     * @return the propertiesString
     */
    public String getPropertiesString() {
        if (crypt()) {
            Map<String, Object> properties = link.getProperties();
            byte[] crypted = JDCrypt.encrypt(JSonStorage.serializeToJson(properties), KEY);
            return CRYPTED + Base64.encodeToString(crypted, false);
        }
        return null;
    }

    /**
     * @param propertiesString
     *            the propertiesString to set
     */
    public void setPropertiesString(String propertiesString) {
        if (propertiesString != null && propertiesString.startsWith(CRYPTED)) {
            byte[] bytes = Base64.decodeFast(propertiesString.substring(CRYPTED.length()));
            Map<String, Object> properties = JSonStorage.restoreFromString(JDCrypt.decrypt(bytes, KEY), new TypeRef<HashMap<String, Object>>() {
            });
            link.setProperties(properties);
        }
    }
}
