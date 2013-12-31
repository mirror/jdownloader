package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 22006 $", interfaceVersion = 2, names = { "bestporntube.me" }, urls = { "http://www.bestporntube.me/video/.*" }, flags = { 0 })
public class BestPornTubeMe extends PluginForHost {

    private String dlUrl = null;

    public BestPornTubeMe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bestporntube.me/static/terms";
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

    private AvailableStatus requestVideo(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Accept-Charset", null);

        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String file_name = br.getRegex("<title>([^<>]*?)- Free Porn Videos and Sex Movies at bestporntube\\.me Kinky Porn Tube</title>").getMatch(0);
        dlUrl = br.getRegex("'file': '(.*?)',").getMatch(0); // get_file/1/00094dfde0d2e240adaf674be13efbd8/0/59/59.flv/";

        if (file_name == null || dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(file_name.replace("\"", "'") + ".flv");
        downloadLink.setUrlDownload(dlUrl);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}