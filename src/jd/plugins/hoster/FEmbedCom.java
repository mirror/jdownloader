package jd.plugins.hoster;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fembed.com" }, urls = { "decryptedforFEmbedHosterPlugin://.*" })
public class FEmbedCom extends PluginForHost {
    public FEmbedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String label = link.getStringProperty("label", null);
        final String type = link.getStringProperty("type", null);
        final String id = link.getStringProperty("fembedid", null);
        final String fembedHost = link.getStringProperty("fembedHost", getHost());
        return fembedHost + "://" + id + "/" + label + "/" + type;
    }

    @Override
    public String getAGBLink() {
        return "https://www.fembed.com/";
    }

    private String  url       = null;
    private boolean resume    = true;
    private int     maxchunks = 1;

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        String url = link.getDownloadURL().replaceFirst("decryptedforFEmbedHosterPlugin://", "https://");
        link.setUrlDownload(url);
    }

    public static final String PROPERTY_DIRECTURL = "directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (checkDirectLink(link) != null) {
            logger.info("Linkcheck via directurly SUCCESS");
            return AvailableStatus.TRUE;
        }
        String fileID = new Regex(link.getPluginPatternMatcher(), "/(?:f|v)/([a-zA-Z0-9_-]+)").getMatch(0);
        final String fembedHost = link.getStringProperty("fembedHost", getHost());
        jd.plugins.decrypter.FEmbedDecrypter.setRequestLimit(fembedHost);
        br.setFollowRedirects(true);
        final PostRequest postRequest = new PostRequest("https://" + fembedHost + "/api/source/" + fileID);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.getPage(postRequest), TypeRef.HASHMAP);
        if (!Boolean.TRUE.equals(response.get("success"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> videos;
        if (response.get("data") instanceof String) {
            videos = (List<Map<String, Object>>) JSonStorage.restoreFromString((String) response.get("data"), TypeRef.OBJECT);
        } else {
            videos = (List<Map<String, Object>>) response.get("data");
        }
        final String searchLabel = link.getStringProperty("label");
        final boolean isDownload = (Thread.currentThread() instanceof SingleDownloadController);
        for (Map<String, Object> video : videos) {
            final String label = (String) video.get("label");
            final String file = (String) video.get("file");
            if (StringUtils.equals(label, searchLabel) && StringUtils.isNotEmpty(file)) {
                url = file;
                if (url.startsWith("/")) {
                    url = "https://www." + fembedHost + url;
                }
                if (!isDownload) {
                    final URLConnectionAdapter con = br.cloneBrowser().openHeadConnection(file);
                    try {
                        if (this.looksLikeDownloadableContent(con)) {
                            if (con.getCompleteContentLength() > 0) {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    } finally {
                        con.disconnect();
                    }
                }
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private String checkDirectLink(final DownloadLink link) {
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link);
            br.clearAuthentications();
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                link.removeProperty(PROPERTY_DIRECTURL);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }
}
