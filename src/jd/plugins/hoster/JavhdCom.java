package jd.plugins.hoster;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * @author noone2407
 */
@HostPlugin(revision = "$Revision:$", interfaceVersion = 2, names = { "javhd.com" }, urls = { "http://(www\\.)?javhd\\.com/(en|jp)/id/\\d+/[a-z-]+" }, flags = { 0 })
public class JavhdCom extends PluginForHost {

    private String dllink = null;

    public JavhdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://javhd.com/en/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.getPage(url);
        String filename = br.getRegex("<h1 id=\"video-title\">(.*?)<\\/h1>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.replaceAll("<i>(.*)</i>", "");
        downloadLink.setFinalFileName(filename + ".mp4");

        String datacode = br.getRegex("http://cdn2\\.javhd\\.com/thumbs/([a-z0-9-]+)/images/\\d+x\\d+/\\d+s\\.jpg").getMatch(0);
        if (datacode == null) {
            return AvailableStatus.FALSE;
        }
        datacode = datacode.substring(0, datacode.length() - 2);
        dllink = "http://wpc.4def.alphacdn.net/802D70B/OriginJHVD/contents/" + datacode + "/videos/" + datacode + "_hq.mp4";
        long filesize = getFileSize(new URL(dllink));
        downloadLink.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(DownloadLink downloadLink) {
    }

    private long getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLengthLong();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }
}
