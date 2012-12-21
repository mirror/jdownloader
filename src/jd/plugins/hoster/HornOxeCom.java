package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision: 18671 $", interfaceVersion = 2, names = { "hornoxe.com" }, urls = { "https?://(www\\.)hornoxedecrypted/.+" }, flags = { 0 })
public class HornOxeCom extends PluginForHost {

    public HornOxeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.hornoxe.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("hornoxedecrypted", "hornoxe.com"));
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        // stupid site doesn't always accept connection
        int repeat = 3;
        for (int i = 0; i <= repeat; i++) {
            Browser br2 = br.cloneBrowser();
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br2, link, link.getStringProperty("DDLink"), true, 1);
                if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video")) {
                    br2.followConnection();
                    if (br2.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.hornoxe.videotemporaryunavailable", "This video is temporary unavailable!"), 60 * 60 * 1000l);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
                break;
            } catch (Throwable e) {
                if (i == repeat && (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video"))) {
                    br2.followConnection();
                    if (br2.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.hornoxe.videotemporaryunavailable", "This video is temporary unavailable!"), 60 * 60 * 1000l);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    continue;
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        String file = br.getRegex("file\":\"(https?://videos\\.hornoxe\\.com/[^\"]+)").getMatch(0);
        if (file == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        URLConnectionAdapter con = null;
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);

        // stupid site doesn't always accept connection
        boolean worked = false;
        int repeat = 3;
        for (int i = 0; i <= repeat; i++) {
            if (worked) break;
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Referer", link.getDownloadURL());
            try {
                con = br2.openGetConnection(file);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("DDLink", br2.getURL());
                    worked = true;
                }
            } catch (Throwable e) {
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            if (!worked) {
                continue;
            }
        }
        if (!worked) {
            return AvailableStatus.UNCHECKED;
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}