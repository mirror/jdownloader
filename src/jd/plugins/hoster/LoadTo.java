//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "load.to" }, urls = { "http://(www\\.)?load\\.to/[A-Za-z0-9]+/" }, flags = { 0 })
public class LoadTo extends PluginForHost {

    public LoadTo(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(10 * 1001l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.load.to/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        workAroundTimeOut(br);
        br.setFollowRedirects(true);
        // Needed to get parallel downloads
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Can\\'t find file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = Encoding.htmlDecode(br.getRegex("<title>([^<>\"]*?) // Load\\.to</title>").getMatch(0));
        final String filesize = br.getRegex("Size: ([^<>\"]*?) <span class=\"space\">").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        /* Link holen */
        String linkurl = getLinkurl();
        br.setFollowRedirects(true);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            // Captcha
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.findID();
            rc.load();
            for (int i = 1; i <= 5; i++) {
                linkurl = getLinkurl();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                final String postData = "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&returnUrl=" + Encoding.urlEncode(br.getURL());
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, postData, true, 1);
                if (dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        rc.reload();
                        continue;
                    }
                }
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, "", true, 1);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() == 416) {
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("file not exist")) logger.info("File maybe offline");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }
        if (!con.isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLinkurl() throws PluginException {
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("\"(http://s\\d+\\.load\\.to/\\?t=\\d+)\"", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<form method=\"post\" action=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return linkurl;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("error", 0);
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* TODO: remove me after 0.9xx public */
    private void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(120 * 1000);
                br.setReadTimeout(120 * 1000);
            }
        } catch (Throwable e) {
        }
    }
}