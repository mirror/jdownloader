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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedload.org" }, urls = { "http://(www\\.)?speedload\\.orgdecrypted/[a-z0-9]+" }, flags = { 2 })
public class SpeedLoadOrg extends PluginForHost {

    public SpeedLoadOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/register." + TYPE);
    }

    // DTemplate Version 0.1.6-psp
    // mods: everything, do NOT upgrade!
    // non account: 1 * 1
    // premium account: 1 * 20
    // protocol: no https
    // captchatype: recaptcha

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms." + TYPE;
    }

    private final String  MAINPAGE  = "http://speedload.org";
    private final String  TYPE      = "php";
    private final boolean RESUME    = true;
    private final int     MAXCHUNKS = 1;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(this.getHost() + "decrypted", this.getHost()));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://speedload.org/api/single_link.php?shortUrl=" + getFID(link));
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(Encoding.htmlDecode(getJson("originalFilename")));
        link.setDownloadSize(Long.parseLong(getJson("fileSize")));
        link.setMD5Hash(getJson("md5"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        boolean captcha = false;
        requestFileInformation(downloadLink);
        int wait = 60;
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
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final Object LOCK = new Object();

    private void login(Account account) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            br.setFollowRedirects(false);
            br.getPage("http://speedload.org/api/user_info.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.containsHTML("User not found")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!"paid user".equals(getJson("level"))) {
            ai.setStatus("Free Accounts are not supported!");
            account.setValid(false);
            return ai;
        }
        ai.setValidUntil(Long.parseLong(getJson("paidExpiryDate")) * 1000l);
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        br.getPage("http://speedload.org/api/single_link.php?shortUrl=" + getFID(link) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        String dllink = getJson("directDownloadLink");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -3);
        if (!dl.getConnection().isContentDisposition()) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
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

}