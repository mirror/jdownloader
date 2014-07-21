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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadlw.com" }, urls = { "http://(www\\.)?uploadlw\\.com/[a-z0-9]+" }, flags = { 0 })
public class UploadLwCom extends PluginForHost {

    public UploadLwCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // For sites which use this script: http://www.yetishare.com/
    // YetiShareBasic Version 0.2.9-psp
    // mods: requestFileInformation [changed fname regex]
    // non account: 1 * 20
    // free account: chunks * maxdls
    // premium account: chunks * maxdls
    // protocol: no https
    // captchatype: null

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms." + TYPE;
    }

    /* Other constants */
    private final String         MAINPAGE                                     = "http://yetishare.com";
    private final String         TYPE                                         = "html";
    private static final String  SIMULTANDLSLIMIT                             = "?e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String  SIMULTANDLSLIMITUSERTEXT                     = "Max. simultan downloads limit reached, wait to start more downloads from this host";
    private static final String  SERVERERROR                                  = "e=Error%3A+Could+not+open+file+for+reading.";
    private static final String  SERVERERRORUSERTEXT                          = "Server error";
    private static final String  WAIT_BETWEEN_DOWNLOADS_LIMIT                 = "e=You+must+wait+";
    private static final String  WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT        = "You must wait between downloads!";
    private static final int     WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final int     ADDITIONAL_WAIT_SECONDS                      = 3;
    /* In case there is no information when accessing the main link */
    private static final boolean AVAILABLE_CHECK_OVER_INFO_PAGE               = false;

    /* Connection stuff */
    private static final boolean FREE_RESUME                                  = true;
    private static final int     FREE_MAXCHUNKS                               = 1;
    private static final int     FREE_MAXDOWNLOADS                            = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        String filesize;
        if (AVAILABLE_CHECK_OVER_INFO_PAGE) {
            br.getPage(link.getDownloadURL() + "~i");
            if (!br.getURL().contains("~i")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("Filename:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)<").getMatch(0);
            if (filename == null || inValidate(Encoding.htmlDecode(filename).trim()) || Encoding.htmlDecode(filename).trim().equals("  ")) {
                /* Filename might not be available here either */
                filename = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            }
            filesize = br.getRegex("Filesize:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.getURL().contains(SIMULTANDLSLIMIT)) {
                link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
                link.getLinkStatus().setStatusText(SIMULTANDLSLIMITUSERTEXT);
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
            if (br.getURL().contains("/error." + TYPE) || br.getURL().contains("/index." + TYPE) || (!br.containsHTML("class=\"downloadPageTable(V2)?\"") && !br.containsHTML("class=\"download\\-timer\""))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)<");
            filename = fInfo.getMatch(0);
            filesize = fInfo.getMatch(1);
            if (filename == null || filesize == null) {
                /* Get piece of the page which usually contains filename- and size */
                final String page_piece = br.getRegex("(<div class=\"contentPageWrapper\">.*?class=\"link btn\\-free\")").getMatch(0);
                if (page_piece != null) {
                    final String endings = jd.plugins.hoster.DirectHTTP.ENDINGS;
                    filename = new Regex(page_piece, "([^<>/\r\n\t:\\?\"]+" + endings + "[^<>/\r\n\t:\\?\"]*)").getMatch(0);
                    filesize = new Regex(page_piece, "(\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))").getMatch(0);
                }
            }
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.replace(",", "")).trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (AVAILABLE_CHECK_OVER_INFO_PAGE) {
            br.getPage(downloadLink.getDownloadURL());
        }
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS);
    }

    public void doFree(final DownloadLink downloadLink, final boolean resume, final int maxchunks) throws Exception, PluginException {
        boolean captcha = false;
        if (br.getURL().contains(SIMULTANDLSLIMIT)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, SIMULTANDLSLIMITUSERTEXT, 1 * 60 * 1000l);
        } else if (br.getURL().contains(WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
            final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes?").getMatch(0);
            if (wait_minutes != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, Integer.parseInt(wait_minutes) * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT * 60 * 1001l);
        } else if (br.getURL().contains(SERVERERROR)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
        }

        /* Handle up to 3 pre-download pages before the (eventually existing) captcha */
        String continue_link = null;
        for (int i = 1; i <= 3; i++) {
            logger.info("Handling pre-download page #" + i);
            continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
            if (continue_link == null && i == 0) {
                continue_link = downloadLink.getDownloadURL() + "?d=1";
                logger.info("Could not find continue_link --> Using standard continue_link, continuing...");
            } else if (continue_link == null && i > 0) {
                logger.info("No continue_link available, stepping out of pre-download loop");
                break;
            } else {
                logger.info("Found continue_link, continuing...");
            }
            final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
            if (waittime != null) {
                logger.info("Found waittime, waiting (seconds): " + waittime + " + " + ADDITIONAL_WAIT_SECONDS + " additional seconds");
                sleep((Integer.parseInt(waittime) + ADDITIONAL_WAIT_SECONDS) * 1001l, downloadLink);
            } else {
                logger.info("Current pre-download page has no waittime");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
            if (dl.getConnection().isContentDisposition()) {
                break;
            }
            br.followConnection();
            if (br.getURL().contains(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* Do not follow connection, already done above */
            final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
            final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
            if (captchaAction == null || rcID == null) {
                logger.warning("Failed to find captcha information");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captcha = true;
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int icaptcha = 1; icaptcha <= 5; icaptcha++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaAction, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resume, maxchunks);
                if (!dl.getConnection().isContentDisposition()) {
                    br.followConnection();
                    if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                    }
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     * 
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }


/* NO OVERRIDE!! We need to stay 0.9*compatible */
public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
return true;
}
}