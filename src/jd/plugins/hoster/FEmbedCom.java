package jd.plugins.hoster;

import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
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
    public String getAGBLink() {
        return "https://www.fembed.com/";
    }

    private String url = null;

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL().replaceFirst("decryptedforFEmbedHosterPlugin://", "https://");
        link.setUrlDownload(url);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String file_id = new Regex(parameter.getPluginPatternMatcher(), "/(?:f|v)/([a-zA-Z0-9_-]+)").getMatch(0);
        final PostRequest postRequest = new PostRequest("https://www.fembed.com/api/sources/" + file_id);
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
        final String searchLabel = parameter.getStringProperty("label", null);
        for (Map<String, Object> video : videos) {
            final String label = (String) video.get("label");
            final String file = (String) video.get("file");
            if (StringUtils.equals(label, searchLabel) && StringUtils.isNotEmpty(file)) {
                url = file;
                if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                    final URLConnectionAdapter con = br.cloneBrowser().openHeadConnection(file);
                    try {
                        if (con.getResponseCode() == 200 && con.getLongContentLength() > 0 && !StringUtils.contains(con.getContentType(), "html")) {
                            parameter.setVerifiedFileSize(con.getCompleteContentLength());
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

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.clearAuthentications();
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
