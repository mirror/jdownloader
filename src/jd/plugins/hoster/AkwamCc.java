package jd.plugins.hoster;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "one.akwam.cc" }, urls = { "https?://(?:one.)?akwam.cc/download/[0-9]+/[0-9]+/?.*" })
public class AkwamCc extends antiDDoSForHost {
    public AkwamCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = BrowserAdapter.openDownload(br, downloadLink, br.getURL(), true, 1);
        dl.startDownload();
    }

    @Override
    public DownloadLink.AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception, PluginException {
        String page = br.getPage(downloadLink.getPluginPatternMatcher());
        String finalLink = new Regex(page, "\"([^\"]+)\" download").getMatch(0);
        downloadLink.setFinalFileName(new Regex(page, "([^/]+)\" download").getMatch(0));
        if (br.getHttpConnection().getResponseCode() == 404) {
            return DownloadLink.AvailableStatus.FALSE;
        }
        if (!StringUtils.isEmpty(finalLink)) {
            URLConnectionAdapter con = null;
            try {
                br.setFollowRedirects(true);
                con = br.openHeadConnection(finalLink);
                if (con.isContentDisposition()) {
                    downloadLink.setDownloadSize(con.getCompleteContentLength());
                    return DownloadLink.AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return DownloadLink.AvailableStatus.FALSE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}