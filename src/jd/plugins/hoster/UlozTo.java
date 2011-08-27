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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to" }, urls = { "http://[\\w\\.]*?((uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/[0-9]+/|bagruj\\.cz/[a-z0-9]{12}/.*?\\.html)" }, flags = { 2 })
public class UlozTo extends PluginForHost {

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uloz.to/kredit/");
    }

    @Override
    public String getAGBLink() {
        return "http://ulozto.net/podminky/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)", "uloz.to"));
    }

    public boolean rewriteHost(DownloadLink link) {
        if (link.getHost().contains("ulozto.sk") || link.getHost().contains("ulozto.cz") || link.getHost().contains("ulozto.net")) {
            correctDownloadLink(link);
            /*
             * not in public version, but rewriteHost is not used in 09580, so
             * no problem
             */
            link.setHost("uloz.to");
            return true;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        handleDownloadUrl(downloadLink);
        String continuePage = br.getRegex("<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
        if (continuePage != null) {
            downloadLink.setUrlDownload(continuePage);
            br.getPage(downloadLink.getDownloadURL());
        }
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("(multipart/form-data|Chybka 404 - požadovaná stránka nebyla nalezena<br>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("\\&t=(.*?)\"")).getMatch(0);
        if (filename == null) filename = br.getRegex(Pattern.compile("cptm=;Pe/\\d+/(.*?)\\?b")).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("style=\"top:\\-55px;\"><div>\\d+:\\d+ \\| (.*?)</div></div>")).getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<span>Velikost:</span> <span class=\"green\">(.*?)</span>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("class=\"info_velikost\" style=\"top:\\-55px;\">[\t\n\r ]+<div>[\t\n\r ]+\\d{2}:\\d{2}(:\\d{2})? \\| (.*?)</div>").getMatch(1);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = null;
        Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            String captchaUrl = br.getRegex(Pattern.compile("style=\"width:175px; height:70px;\" width=\"175\" height=\"70\" src=\"(http://.*?)\"")).getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex(Pattern.compile("\"(http://img\\.uloz\\.to/captcha/\\d+\\.png)\"")).getMatch(0);
            Form captchaForm = br.getFormbyProperty("name", "dwn");
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("captcha_user", code);
            captchaForm.remove(null);
            br.submitForm(captchaForm);
            dllink = br.getRedirectLocation();
            if (dllink == null) break;
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    failed = false;
                    break;
                } else {
                    br.clearCookies("http://www.uloz.to/");
                    handleDownloadUrl(downloadLink);
                    continue;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.contains("/error404/?fid=file_not_found")) {
            logger.info("The user entered the correct captcha but this file is offline...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleDownloadUrl(DownloadLink downloadLink) throws IOException {
        br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(".*?bagruj\\.cz/[a-z0-9]{12}.*?") && br.getRedirectLocation() != null) {
            downloadLink.setUrlDownload(br.getRedirectLocation());
            br.getPage(downloadLink.getDownloadURL());
        } else if (br.getRedirectLocation() != null) {
            logger.info("Getting redirect-page");
            br.getPage(br.getRedirectLocation());
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.postPage("http://www.uloz.to/?do=authForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&trvale=on&login=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://uloz.to/", "autologin") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://www.uloz.to/nastaveni/");
        String trafficleft = br.getRegex("<td>Kredit:</td><td style=\"text-align: right; font-weight: bold;\">(.*?)</td></tr>").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("class=\"credit\"><a href=\"/kredit/\" class=\"coins\" title=\"(.*?) = ").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        String dllink = br.getRegex("\\(\\'/downloadsvip/\\'\\);\" style=\"margin: 26px 0pt 0pt 82px; float: none; color: #6D0D4C;\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://dla\\d+\\.uloz\\.to/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
