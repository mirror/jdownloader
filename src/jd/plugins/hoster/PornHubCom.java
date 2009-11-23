package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision: 9543 $", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "http://[\\w\\.]*?pornhub\\.com/view_video\\.php\\?viewkey=[a-z0-9]+" }, flags = { 0 })
public class PornHubCom extends PluginForHost {

    private static String post_element = "add299463d4410c6d1b1c418868225f7";

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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        String file_name = br.getRegex("video-title-nf\">.*<h1>(.*?)</h1>").getMatch(0);
        if (file_name == null) { return AvailableStatus.FALSE; }
        String[] linksplit = downloadLink.getDownloadURL().split("=");
        String video_id = linksplit[linksplit.length - 1];

        byte[] b = new byte[] { 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x0c, 0x70, 0x6c, 0x61, 0x79, 0x65, 0x72, 0x43, 0x6f, 0x6e, 0x66, 0x69, 0x67, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x44, 0x0a, 0x00, 0x00, 0x00, 0x03, 0x02, 0x00 };
        byte[] b2 = new byte[] { 0x02, 0x00, 0x02, 0x2d, 0x31, 0x02, 0x00, 0x20 };

        String url = "http://www.pornhub.com//gateway.php";
        String postdata = new String(b) + new String(new byte[] { (byte) video_id.length() }) + video_id + new String(b2) + post_element;

        br.getHeaders().put("Content-Type", "application/x-amf");
        br.setFollowRedirects(false);
        br.postPageRaw(url, postdata);

        String file_url = br.getRegex("flv_url.*[i|j](http.*?)\\?r=.*").getMatch(0);
        linksplit = file_url.split("\\.");
        downloadLink.setFinalFileName(file_name + "." + linksplit[linksplit.length - 1]);
        downloadLink.setUrlDownload(file_url);

        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
                long size = br.getHttpConnection().getLongContentLength();
                downloadLink.setDownloadSize(Long.valueOf(size));
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
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
