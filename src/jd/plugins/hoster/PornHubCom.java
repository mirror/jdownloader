package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "http://[\\w\\.]*?pornhub\\.com/view_video\\.php\\?viewkey=[a-z0-9]+" }, flags = { 0 })
public class PornHubCom extends PluginForHost {

    private static String post_element = "add299463d4410c6d1b1c418868225f7";
    private String dlUrl = null;

    public PornHubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhub.com/terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    private AvailableStatus requestVideo(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        dlUrl = null;
        String file_name = br.getRegex("video-title-nf\" style=\"height:[0-9]+px;\">.*<h1>(.*?)</h1>").getMatch(0);
        if (file_name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] linksplit = downloadLink.getDownloadURL().split("=");
        String video_id = linksplit[linksplit.length - 1];

        byte[] b = new byte[] { 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x0c, 0x70, 0x6c, 0x61, 0x79, 0x65, 0x72, 0x43, 0x6f, 0x6e, 0x66, 0x69, 0x67, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x44, 0x0a, 0x00, 0x00, 0x00, 0x03, 0x02, 0x00 };
        byte[] b2 = new byte[] { 0x02, 0x00, 0x02, 0x2d, 0x31, 0x02, 0x00, 0x20 };

        String url = "http://www.pornhub.com//gateway.php";
        String postdata = new String(b) + new String(new byte[] { (byte) video_id.length() }) + video_id + new String(b2) + post_element;

        br.getHeaders().put("Content-Type", "application/x-amf");
        br.setFollowRedirects(false);
        br.postPageRaw(url, postdata);

        dlUrl = br.getRegex("flv_url.*(http.*?)\\?r=.*").getMatch(0);
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        linksplit = dlUrl.split("\\.");
        downloadLink.setFinalFileName(file_name + "." + linksplit[linksplit.length - 1]);
        return AvailableStatus.TRUE;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(dlUrl).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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