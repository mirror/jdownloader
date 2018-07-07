package jd.plugins.hoster;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "fembed.com" }, urls = { "decryptedforFEmbedHosterPlugin://.*" })
public class FEmbedCom extends PluginForHost {
    public FEmbedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.fembed.com/";
    }

    private String data = "";
    private String url;

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL().replaceFirst("decryptedforFEmbedHosterPlugin://", "https://");
        link.setUrlDownload(url);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (parameter.getDownloadSize() > 0 && url != null && !url.isEmpty()) {
            return AvailableStatus.TRUE;
        }
        String file_id = new Regex(parameter.getPluginPatternMatcher(), "/(?:f|v)/([a-zA-Z0-9_-]+)$").getMatch(0);
        final PostRequest postRequest = new PostRequest("https://www.fembed.com/api/source/" + file_id);
        data = br.getPage(postRequest);
        String success = PluginJSonUtils.getJson(PluginJSonUtils.unescape(data), "success");
        if (success == "false") {
            return AvailableStatus.FALSE;
        }
        String label = parameter.getStringProperty("label");
        String data2 = PluginJSonUtils.getJson(PluginJSonUtils.unescape(data), "data");
        String[] data2_ar = PluginJSonUtils.getJsonResultsFromArray(data2);
        int index = 0;
        int cur = -1;
        for (String ar : data2_ar) {
            String url2 = PluginJSonUtils.getJson(ar, "file");
            String label2 = PluginJSonUtils.getJson(ar, "label");
            if (label.equals(label2) || data2_ar.length == 1) {
                cur = index;
                url = url2;
                URLConnectionAdapter con = null;
                con = this.br.openHeadConnection(url2);
                if (!con.getContentType().contains("html")) {
                    long size = con.getLongContentLength();
                    parameter.setDownloadSize(size);
                }
                con.disconnect();
                break;
            }
            index++;
        }
        data = br.getPage(postRequest);
        if (cur != -1) {
            url = PluginJSonUtils.getJson(PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJson(PluginJSonUtils.unescape(data), "data"))[cur], "file");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.clearAuthentications();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);
        url = null;
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
