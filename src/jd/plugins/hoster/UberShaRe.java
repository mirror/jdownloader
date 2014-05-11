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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uber-sha.re" }, urls = { "https://(www\\.)?uber\\-sha\\.re/(?!faq|register|login|terms|report_file)[a-z0-9]+" }, flags = { 0 })
public class UberShaRe extends PluginForHost {

    public UberShaRe(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DTemplate Version 0.2.1-psp
    // mods:
    // non account: chunks * maxdls
    // premium account: chunks * maxdls
    // protocol: no https
    // captchatype: null

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms." + TYPE;
    }

    private final String        MAINPAGE                                     = "http://uber-sha.re";
    private final String        TYPE                                         = "html";
    private final boolean       RESUME                                       = true;
    private final int           MAXCHUNKS                                    = 0;
    private static final String SIMULTANDLS_LIMIT                            = "?e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String SIMULTANDLSLIMIT_USERTEXT                    = "Max. simultan downloads limit reached, wait to start more downloads from this host";
    private static final String WAIT_BETWEEN_DOWNLOADS_LIMIT                 = "e=You+must+wait+";
    private static final String WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT        = "You must wait between downloads!";
    private static final int    WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final String SERVERERROR                                  = "e=Error%3A+Could+not+open+file+for+reading.";
    private static final String SERVERERRORUSERTEXT                          = "Server error";

    public void correctDownloadLink(DownloadLink link) {
        /* In case we add links via decrypter */
        link.setUrlDownload(link.getDownloadURL().replace(this.getHost() + "decrypted", this.getHost()));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains(SIMULTANDLS_LIMIT)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(SIMULTANDLSLIMIT_USERTEXT);
            return AvailableStatus.TRUE;
        } else if (br.getURL().contains(WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT);
            return AvailableStatus.TRUE;
        } else if (br.getURL().contains(SERVERERROR)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(SERVERERRORUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.getURL().contains("/error." + TYPE) || br.getURL().contains("/index." + TYPE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("<th class=\"descr\"([^<>]*?)?>[\t\n\r ]+<strong>([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)<br/>");
        final String filename = fInfo.getMatch(1);
        final String filesize = fInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        boolean captcha = false;
        requestFileInformation(downloadLink);
        if (br.getURL().contains(SIMULTANDLS_LIMIT)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, SIMULTANDLSLIMIT_USERTEXT, 1 * 60 * 1000l);
        } else if (br.getURL().contains(WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
            final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes").getMatch(0);
            if (wait_minutes != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, Integer.parseInt(wait_minutes) * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT * 60 * 1001l);
        } else if (br.getURL().contains(SERVERERROR)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l); }
        final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (waittime != null) sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link != null) {
            logger.info("Found continue_link");
        } else {
            continue_link = downloadLink.getDownloadURL() + "?d=1";
            logger.info("Could not find continue_link --> Using standard continue_link");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, RESUME, MAXCHUNKS);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getURL().contains(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
            final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
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
                    if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                    rc.reload();
                    continue;
                }
                break;
            }
            captcha = true;
        }
        if (!dl.getConnection().isContentDisposition()) {
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}