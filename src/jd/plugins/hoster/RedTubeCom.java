package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redtube.com" }, urls = { "http://(www\\.)?redtube(\\.cn)?\\.com/\\d+" }, flags = { 0 })
public class RedTubeCom extends PluginForHost {
    private String dlink = null;

    public RedTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.redtube.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.redtube.com", "language", "en");
        try {
            br.getPage(link.getDownloadURL().toLowerCase());
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Offline link
        if (br.containsHTML("is no longer available") || br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Invalid link
        if (br.containsHTML(">Error Page Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = br.getRegex("<h1 class=\"videoTitle[^>]+>(.*?)</h1>").getMatch(0);
        if (fileName == null) fileName = br.getRegex("<title>(.*?) (-|\\|) RedTube[^<]+</title>").getMatch(0);
        br.setFollowRedirects(true);
        dlink = br.getRegex("html5_vid.*?source src=\"(http.*?)(\"|%3D%22)").getMatch(0);
        if (dlink == null) dlink = br.getRegex("flv_h264_url=(http.*?)(\"|%3D%22)").getMatch(0);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlink = Encoding.urlDecode(dlink, true);
        String ext = new Regex(dlink, "(\\.flv|\\.mp4).+$").getMatch(0);
        if (fileName != null || ext != null) link.setName(fileName.trim() + ext);
        try {
            if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());

                return AvailableStatus.TRUE;
            }
        } finally {
            try {
                br.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("redtube\\.cn\\.com", "redtube.com"));
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}