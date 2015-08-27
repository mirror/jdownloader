package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Files;

@HostPlugin(revision = "$Revision: 25467 $", interfaceVersion = 2, names = { "tvtotal.prosieben.de" }, urls = { "http://tvtotal\\.prosieben\\.de/videos/.*?/\\d+/" }, flags = { 2 })
public class TvTotalPro7 extends PluginForHost {

    public TvTotalPro7(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tvtotal.prosieben.de/infos/datenschutz/";
    }

    private String DLLINK = null;

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String ID = new Regex(link.getPluginPatternMatcher(), "/(\\d+)/$").getMatch(0);
        DLLINK = br.getRegex("videoURL: '(https?://.*?/" + ID + "/" + ID + ".*?)'").getMatch(0);
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("og:title\"\\s*?content=\"(.*?)\"").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) {
            final String fileName = title + "." + Files.getExtension(DLLINK);
            link.setFinalFileName(fileName);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
        DLLINK = null;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
