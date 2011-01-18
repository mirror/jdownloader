package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.google.com" }, urls = { "http://[\\w\\.]*?video\\.google\\.com/videoplay\\?docid=-?\\d+" }, flags = { 0 })
public class VideoGoogle extends PluginForHost {

    public VideoGoogle(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.google.com/accounts/TOS?loc=ZZ&hl=en";
    }

    private String DLLINK = null;

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, DLLINK, true, 0);
        if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.videogoogle.videotemporaryunavailable", "This video is temporary unavailable!"), 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String name = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (br.containsHTML("but this video may not be availabl")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (name == null || "Google Videos Error".equals(name)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(Encoding.htmlDecode(name) + ".flv");
        DLLINK = br.getRegex("videoUrl\\\\x3d(http://.*?\\.googlevideo\\.com/videoplayback.*?)\\\\x26thumbnailUrl").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.urlDecode(DLLINK, true);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                parameter.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
