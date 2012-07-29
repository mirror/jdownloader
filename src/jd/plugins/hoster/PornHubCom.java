package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "http://(www\\.)?((de|fr|it|es|pt)\\.)?pornhub\\.com/view_video\\.php\\?viewkey=[a-z0-9]+" }, flags = { 0 })
public class PornHubCom extends PluginForHost {

    private static String post_element = "add299463d4410c6d1b1c418868225f7";
    private String        dlUrl        = null;

    public PornHubCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("((de|fr|it|es|pt)\\.)pornhub\\.com/", "pornhub.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhub.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestVideo(link);
        if (dlUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(dlUrl).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private AvailableStatus requestVideo(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setCookie("http://pornhub.com/", "age_verified", "1");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.pornhub.com/") || !br.containsHTML("\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dlUrl = null;
        String file_name = br.getRegex("class=\"section_bar\"><h1 class=\"section_title\">([^<>/]*?)</h1>").getMatch(0);
        if (file_name == null) file_name = br.getRegex("<title([^<>/]*?) \\- Pornhub\\.com</title>").getMatch(0);
        if (file_name == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String[] linksplit = downloadLink.getDownloadURL().split("=");
        final String video_id = linksplit[linksplit.length - 1];

        final byte[] b = new byte[] { 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x0c, 0x70, 0x6c, 0x61, 0x79, 0x65, 0x72, 0x43, 0x6f, 0x6e, 0x66, 0x69, 0x67, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x44, 0x0a, 0x00, 0x00, 0x00, 0x03, 0x02, 0x00 };
        final byte[] b2 = new byte[] { 0x02, 0x00, 0x02, 0x2d, 0x31, 0x02, 0x00, 0x20 };

        final String url = "http://www.pornhub.com//gateway.php";
        final String postdata = new String(b) + new String(new byte[] { (byte) video_id.length() }) + video_id + new String(b2) + post_element;

        br.getHeaders().put("Content-Type", "application/x-amf");
        br.setFollowRedirects(false);
        br.postPageRaw(url, postdata);

        final byte[] raw = br.toString().getBytes();
        if (raw == null || raw.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        for (int i = 0; i <= raw.length - 1; i++) {
            if (raw[i] < 32 || raw[i] > 176) {
                raw[i] = 35; // #
            }
        }

        dlUrl = new Regex(new String(raw), "flv_url.*?(http.*?)##post_roll").getMatch(0);
        if (dlUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        linksplit = dlUrl.split("\\.");
        downloadLink.setFinalFileName(file_name.replace("\"", "'") + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}