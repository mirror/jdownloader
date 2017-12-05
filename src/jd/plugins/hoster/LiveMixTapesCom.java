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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "http://(\\w+\\.)?(livemixtapesdecrypted\\.com/download(/mp3)?/\\d+/.*?\\.html|club\\.livemixtapes\\.com/play/\\d+)" })
public class LiveMixTapesCom extends PluginForHost {
    private static final String CAPTCHATEXT            = "/captcha/captcha\\.gif\\?";
    private static final String MAINPAGE               = "http://www.livemixtapes.com/";
    private static final String MUSTBELOGGEDIN         = ">You must be logged in to access this page";
    private static final String ONLYREGISTEREDUSERTEXT = "Download is only available for registered users";
    private static final String TYPE_REDIRECTLINK      = "http://(www\\.)?livemixtap\\.es/[a-z0-9]+";
    private static final String TYPE_DIRECTLINK        = "http://(www\\.)?club\\.livemixtapes\\.com/play/\\d+";

    public LiveMixTapesCom(PluginWrapper wrapper) {
        super(wrapper);
        // Currently there is only support for free accounts
        this.enablePremium("http://www.livemixtapes.com/signup.html");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("livemixtapesdecrypted.com/", "livemixtapes.com/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(TYPE_DIRECTLINK)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getDownloadURL());
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("(>Not Found</|The page you requested could not be found\\.<|>This mixtape is no longer available for download.<)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = null, filesize = null;
            if (br.containsHTML(MUSTBELOGGEDIN)) {
                final Regex fileInfo = br.getRegex("<td height=\"35\"><div style=\"padding\\-left: 8px\">([^<>\"]*?)</div></td>[\t\n\r ]+<td align=\"center\">([^<>\"]*?)</td>");
                filename = fileInfo.getMatch(0);
                filesize = fileInfo.getMatch(1);
                if (filename == null || filesize == null) {
                    link.getLinkStatus().setStatusText(ONLYREGISTEREDUSERTEXT);
                    return AvailableStatus.TRUE;
                }
            } else {
                final String timeRemaining = br.getRegex("TimeRemaining = (\\d+);").getMatch(0);
                if (timeRemaining != null) {
                    link.getLinkStatus().setStatusText("Not yet released, cannot download");
                    link.setName(Encoding.htmlDecode(br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0)));
                    return AvailableStatus.TRUE;
                }
                final Regex fileInfo = br.getRegex("<td height=\"35\"><div[^>]+>(.*?)</div></td>[\t\n\r ]+<td align=\"center\">((\\d+(\\.\\d+)? ?(KB|MB|GB)))</td>");
                filename = fileInfo.getMatch(0);
                filesize = fileInfo.getMatch(1);
            }
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.setFollowRedirects(false);
        String dllink = null;
        boolean resume;
        int maxChunks;
        if (downloadLink.getDownloadURL().matches(TYPE_DIRECTLINK)) {
            dllink = downloadLink.getDownloadURL();
            resume = true;
            maxChunks = 0;
        } else {
            resume = false;
            maxChunks = 1;
            if (br.containsHTML(MUSTBELOGGEDIN)) {
                final Browser br2 = br.cloneBrowser();
                try {
                    br2.getPage("http://www.livemixtapes.com/play/" + new Regex(downloadLink.getDownloadURL(), "download(/mp3)?/(\\d+)").getMatch(1));
                    dllink = br2.getRedirectLocation();
                } catch (final Exception e) {
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.livemixtapescom.only4registered", ONLYREGISTEREDUSERTEXT));
                }
            } else {
                final String timeRemaining = br.getRegex("TimeRemaining = (\\d+);").getMatch(0);
                if (timeRemaining != null) {
                    downloadLink.getLinkStatus().setStatusText("Not yet released, cannot download");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                final String timestamp = br.getRegex("name=\"timestamp\" value=\"(\\d+)\"").getMatch(0);
                if (timestamp == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br.containsHTML("<img src=\"/captcha/captcha\\.gif\\?\\d+")) {
                    String captcha = br.getRegex("(/captcha/captcha\\.gif\\?\\d+)").getMatch(0);
                    String code = getCaptchaCode(captcha, downloadLink);
                    if (captcha == null || code == null || code.equals("")) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.postPage(br.getURL(), "retries=0&timestamp=" + timestamp + "&code=" + code);
                    if (br.containsHTML("<img src=\"/captcha/captcha\\.gif\\?\\d+")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                    final String challengekey = br.getRegex("ACPuzzle\\.create\\(\\'(.*?)\\'").getMatch(0);
                    if (challengekey == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    sm.setChallengeKey(challengekey);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                    final String chid = sm.getChallenge(code);
                    // Usually we have a waittime here but it can be skipped
                    // int waittime = 40;
                    // String wait =
                    // br.getRegex("<span id=\"counter\">(\\d+)</span>").getMatch(0);
                    // if (wait == null) wait =
                    // br.getRegex("wait: (\\d+)").getMatch(0);
                    // if (wait != null) {
                    // waittime = Integer.parseInt(wait);
                    // if (waittime > 1000) waittime = waittime / 1000;
                    // sleep(waittime * 1001, downloadLink);
                    // }
                    try {
                        br.postPage(br.getURL(), "retries=0&timestamp=" + timestamp + "&adcopy_response=manual_challenge&adcopy_challenge=" + chid);
                    } catch (Exception e) {
                    }
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.livemixtapes.com/contact.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        doFree(link);
    }

    public void login(final Browser br, final Account account) throws Exception {
        this.setBrowserExclusive();
        // br.getPage(MAINPAGE);
        br.postPage("https://www.livemixtapes.com/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "u") == null || br.getCookie(MAINPAGE, "p") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public void reset() {
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