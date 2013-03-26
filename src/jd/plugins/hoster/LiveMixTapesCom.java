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

import java.io.IOException;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "http://(www\\.)?(livemixtap\\.es/[a-z0-9]+|(indy\\.)?livemixtapes\\.com/(download(/mp3)?|mixtapes)/\\d+/.*?\\.html)" }, flags = { 2 })
public class LiveMixTapesCom extends PluginForHost {

    private static final String CAPTCHATEXT            = "/captcha/captcha\\.gif\\?";
    private static final String MAINPAGE               = "http://www.livemixtapes.com/";
    private static final String MUSTBELOGGEDIN         = ">You must be logged in to access this page";
    private static final String ONLYREGISTEREDUSERTEXT = "Download is only available for registered users";

    public LiveMixTapesCom(PluginWrapper wrapper) {
        super(wrapper);
        // Currently there is only support for free accounts
        this.enablePremium("http://www.livemixtapes.com/signup.html");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("indy.", "").replace("/mixtapes/", "/download/"));
    }

    private void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String captchaUrl = br.getRegex("\"(/captcha/captcha\\.gif\\?\\d+)\"").getMatch(0);
        if (captchaUrl == null) captchaUrl = br.getRegex("<td width=\"200\">[\t\n\r ]+<img src=\"(/.*?)\"").getMatch(0);
        if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaUrl = "http://www.livemixtapes.com" + captchaUrl;
        String code = getCaptchaCode(captchaUrl, downloadLink);
        // Usually we have a waittime here but it can be skipped
        // int waittime = 40;
        // String wait =
        // br.getRegex("<span id=\"counter\">(\\d+)</span>").getMatch(0);
        // if (wait == null) wait = br.getRegex("wait: (\\d+)").getMatch(0);
        // if (wait != null) {
        // waittime = Integer.parseInt(wait);
        // if (waittime > 1000) waittime = waittime / 1000;
        // sleep(waittime * 1001, downloadLink);
        // }
        br.setFollowRedirects(false);
        try {
            br.postPage(br.getURL(), "code=" + code);
        } catch (Exception e) {
        }
        if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
            login(account);
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
        if (br.containsHTML(MUSTBELOGGEDIN)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.livemixtapescom.only4registered", ONLYREGISTEREDUSERTEXT));
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        doFree(link);
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // br.getPage(MAINPAGE);
        br.postPage("http://www.livemixtapes.com/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "u") == null || br.getCookie(MAINPAGE, "p") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        /** If link is a short link correct it */
        if (new Regex(link.getDownloadURL(), "http://(www\\.)?livemixtap\\.es/[a-z0-9]+").matches()) {
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            String correctLink = br.getRedirectLocation();
            if (correctLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(correctLink);
            correctLink = br.getRedirectLocation();
            if (correctLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setUrlDownload(correctLink);
            correctDownloadLink(link);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Not Found</|The page you requested could not be found\\.<|>This mixtape is no longer available for download.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(MUSTBELOGGEDIN)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.livemixtapescom.only4registered", ONLYREGISTEREDUSERTEXT));
            return AvailableStatus.TRUE;
        }
        Regex fileInfo = br.getRegex("<td height=\"35\">\\&nbsp;\\&nbsp;\\&nbsp;(.*?)</td>[\t\n\r ]+<td align=\"center\">(.*?)</td>");
        final String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
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