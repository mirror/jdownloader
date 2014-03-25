//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fastfileshare.com.ar" }, urls = { "http://(www\\.)?fastfileshare\\.com\\.ar/index\\.php\\?p=download\\&hash=[A-Za-z0-9]+" }, flags = { 2 })
public class FastFileShareComAr extends PluginForHost {

    private static final String COOKIE_HOST = "http://fastfileshare.com.ar";

    public FastFileShareComAr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fastfileshare.com.ar/index.php?p=fastpass&");
    }

    public void correctDownloadLink(DownloadLink link) {
        // Use the english language
        link.setUrlDownload(link.getDownloadURL() + "&langSwitch=english&");
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
        String expires = br.getRegex("Your Account Expires on (.*?, \\d+), ").getMatch(0);
        if (expires != null) {
            // Your Account Expires on July 3, 2010, 1:26 pm<br>
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "MMMM d, yyyy", null));
        }
        ai.setStatus("Premium User");
        account.setValid(true);

        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://fastfileshare.com.ar/index.php?p=rules";
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">This file has been password protected by the uploader")) throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not supported yet");
        br.setFollowRedirects(false);
        String dllink = null;
        boolean captcha = true;
        Form dlform = null;
        for (int i = 0; i <= 5; i++) {
            String captchaurl = br.getRegex("class=\"captchapict\" src=\"(.*?)\"").getMatch(0);
            if (captchaurl == null) {
                logger.info("Captcharegex 1 failed!");
                captchaurl = br.getRegex("\"(http://fastfileshare\\.com\\.ar/temp/[a-z0-9]+\\.jpg)\"").getMatch(0);
            }
            if (captchaurl == null && i == 0) {
                captcha = false;
                break;
            } else {
                dlform = br.getForm(0);
                if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode(captchaurl, downloadLink);
                dlform.put("private_key", code);
                br.submitForm(dlform);
                dllink = br.getRedirectLocation();
                if (dllink == null) continue;
                break;
            }
        }
        if (!captcha) {
            br.postPage(br.getURL(), "waited=yes&pass_test={PASS}");
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        if (br.containsHTML(">This file has been password protected by the uploader")) throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not supported yet");
        login(account);
        br.setFollowRedirects(false);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        Form dlform = br.getForm(0);
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(dlform);
        String finalLink = br.getRegex("<code>Your Download Link: <a href=\"(http://.*?)\"").getMatch(0);
        if (finalLink == null) finalLink = br.getRegex("\"(http://d\\d+\\.fastfileshare\\.com\\.ar:\\d+/index\\.php\\?p=.*?\\&link=\\d+&name=.*?)\"").getMatch(0);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, finalLink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(COOKIE_HOST + "/index.php?p=login&langSwitch=english&");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        // If the referer is still in the form (and if it is a valid
        // downloadlink) the download starts directly after logging in so we
        // MUST remove it!
        form.remove("refer_url");
        form.put("autologin", "0");
        br.submitForm(form);
        br.getPage(COOKIE_HOST + "/index.php?p=points&langSwitch=english&");
        if (!br.containsHTML("Your Account Expires on")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setName(new Regex(link.getDownloadURL(), "hash=([A-Za-z0-9]+)").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("('File Not Found|The specified file can not found in our servers)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">This file has been password protected by the uploader")) {
            link.getLinkStatus().setStatusText("Link is password protected");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<strong>Filename:</strong></div></td>.*?<td width=\"\\d+%\"><div align=\"left\" class=\"style47\">(.*?)<img").getMatch(0);
        String filesize = br.getRegex("<strong>Filesize:</strong></div></td>.*?<td><div align=\"left\"  class=\"style47\">(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String md5 = br.getRegex("<strong>Suma MD5: </strong></div></td>.*?<td width=\"\\d+%\"><div align=\"left\" class=\"style47\">(.*?)</div>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
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