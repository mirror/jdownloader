//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "clipshouse.com" }, urls = { "http://(www\\.)?clipshouse\\.com/(?!faq|register|login|terms|report_file)[a-z0-9]+" }, flags = { 2 })
public class ClipsHouseCom extends PluginForHost {

    public ClipsHouseCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/register.html");
    }

    // DTemplate Version 0.1.6-psp
    // mods: heavily modified, do NOT upgrade!
    // non account: 1 * 1
    // premium account: chunks * maxdls
    // protocol: no https
    // captchatype: null

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms.html";
    }

    private final String  MAINPAGE  = "http://clipshouse.com";
    private final String  TYPE      = "html";
    private final boolean RESUME    = false;
    private final int     MAXCHUNKS = 1;
    private boolean       DIRECT    = false;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(this.getHost() + "decrypted", this.getHost()));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                DIRECT = true;
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.getURL().contains("/error." + TYPE) || br.getURL().contains("/index." + TYPE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("width=\\'32\\' height=\\'32\\'  /><span > ([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)</span></h2>");
        final String filename = fInfo.getMatch(0);
        final String filesize = fInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        boolean captcha = false;
        requestFileInformation(downloadLink);
        if (DIRECT) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), RESUME, MAXCHUNKS);
        } else {
            final String dllink = br.getRegex("\"(http://media\\d+\\.clipshouse\\.com:\\d+/[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, RESUME, MAXCHUNKS);
            } else {
                final String reconnectWait = br.getRegex("r<br /> Please wait (\\d+) Minutes").getMatch(0);
                if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 60 * 1001l);
                int wait = 10;
                final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
                if (waittime != null) wait = Integer.parseInt(waittime);
                sleep(wait * 1001l, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL() + "?d=1", RESUME, MAXCHUNKS);
                if (!dl.getConnection().isContentDisposition()) {
                    br.followConnection();
                    final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(http://[^<>\"]*?)\"").getMatch(0);
                    final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null || captchaAction == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    for (int i = 0; i <= 5; i++) {
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaAction, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, RESUME, MAXCHUNKS);
                        if (!dl.getConnection().isContentDisposition()) {
                            br.followConnection();
                            rc.reload();
                            continue;
                        }
                        break;
                    }
                    captcha = true;
                }
            }
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}