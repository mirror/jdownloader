//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.com" }, urls = { "https?://(?:\\w+\\.)*box\\.(?:com|net)/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+" })
public class BoxCom extends antiDDoSForHost {

    private static final String TOS_LINK                = "https://www.box.net/static/html/terms.html";

    private static final String fileLink                = "https?://(?:\\w+\\.)*box\\.com/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+";

    private String              dllink                  = null;
    private boolean             error_message_bandwidth = false;

    public BoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return TOS_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("box.net/", "box.com/"));
    }

    @Override
    public String rewriteHost(String host) {
        if ("box.net".equals(getHost())) {
            if (host == null || "box.net".equals(host)) {
                return "box.com";
            }
        }
        return super.rewriteHost(host);
    }

    public static boolean isOffline(Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Box \\| 404 Page Not Found</title>") || br.containsHTML("error_message_not_found")) {
            return true;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // our default is german, this returns german!!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        correctDownloadLink(parameter);
        if (parameter.getPluginPatternMatcher().matches(fileLink)) {
            final Regex dlIds = new Regex(parameter.getPluginPatternMatcher(), "box\\.com/s/([a-z0-9]+)/file/(\\d+)");
            final String sharedname = dlIds.getMatch(0);
            final String fileid = dlIds.getMatch(1);
            br.getPage(parameter.getPluginPatternMatcher());
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String results = br.getRegex("<script type=\"text/x-config\">\\s*.*?\"fileName\".*?</script>").getMatch(-1);
            if (results == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String filename = PluginJSonUtils.getJson(results, "fileName");
            final String filesize = br.getRegex("Size:\\s*([\\d\\.]+\\s*[KMGT]{0,1}B)").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            parameter.setName(filename);
            if (filesize != null) {
                parameter.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            parameter.setLinkID("box.com://file/" + fileid);
            dllink = "https://www.box.com/index.php?rm=box_download_shared_file&shared_name=" + sharedname + "&file_id=f_" + fileid;
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.FALSE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (error_message_bandwidth) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The uploader of this file doesn't have enough bandwidth left!", 20 * 60 * 1000l);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            logger.info("The final downloadlink seems not to be a file");
            br.followConnection();
            if (br.containsHTML("error_message_bandwidth")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The uploader of this file doesn't have enough bandwidth left!", 3 * 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}