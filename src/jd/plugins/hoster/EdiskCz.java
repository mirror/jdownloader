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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "edisk.cz" }, urls = { "http://(?:www\\.)?edisk\\.(?:cz|sk|eu)/(?:[a-z]{2}/)?(?:stahni|download)/[0-9]+/.+\\.html" })
public class EdiskCz extends PluginForHost {
    private static final String MAINPAGE = "https://www.edisk.eu/";

    public EdiskCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.edisk.eu/credit/");
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getDownloadURL(), "(stahni|download)/(.+)").getMatch(1);
        link.setUrlDownload("http://www.edisk.eu/download/" + linkpart);
    }

    @Override
    public String getAGBLink() {
        return "https://www.edisk.eu/podminky-pouziti-sluzby-1/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        prepBr();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("id=\"error_msg\"|>Tento soubor již neexistuje|>This file does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2016-11-23: New */
        final Regex fileinfo = br.getRegex("<h1>([^<>\"]+) \\((\\d+[^<>\"]+)\\)</h1>");
        String filename = fileinfo.getMatch(0);
        if (filename == null) {
            filename = br.getRegex("/filetypes/[a-z0-9]+\\.png\" alt=\"([^<>\"]+)\"").getMatch(0);
        }
        String filesize = fileinfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * Set the final filename here because server gives us filename + ".html" which is bad
         */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    /* TODO: Implement English(and missing) errormessages */
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        final String url_download = br.getURL();
        final String fid = br.getRegex("data\\.filesId\\s*?=\\s*?(\\d+);").getMatch(0);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        /* Critical header!! */
        br.getHeaders().put("Requested-With-AngularJS", "true");
        // br.getPage("/files/downloadslow/" + fid);
        br.postPageRaw("/ajax/generatecaptcha", "{\"url\":\"/files/downloadslow/" + fid + "/\"}");
        final String captchaurl = PluginJSonUtils.getJsonValue(br, "captcha");
        if (captchaurl == null || captchaurl.equals("")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String code = this.getCaptchaCode(captchaurl, downloadLink);
        br.postPageRaw(url_download, "{\"triplussest\":\"devÄt\",\"captcha_id\":\"/files/downloadslow/" + fid + "/\",\"captcha\":\"" + code + "\"}");
        String dllink = PluginJSonUtils.getJsonValue(br, "redirect");
        final String redirect_because_of_invalid_captcha = PluginJSonUtils.getJsonValue(br, "msg");
        if ((dllink == null || dllink.equals("")) && redirect_because_of_invalid_captcha != null) {
            /* E.g. {"type":"json","msg":"Neplatn\u00fd captcha k\u00f3d","msgtype":"danger","error":true} */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("http") && !dllink.startsWith("/")) {
            dllink = "/" + dllink;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultaneous downloads", 10 * 60 * 1000l);
            } else if (br.containsHTML("Pomalu je možné stáhnout pouze 1 soubor") || br.getURL().contains("/kredit")) {
                /*
                 * E.g. "<p>Pomalu je možné stáhnout pouze 1 soubor / 24 hodin. Pro stažení dalšího souboru si musíš <a href="/kredit/
                 * ">koupit kredit</a>.</p>"
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 3 * 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* 2016-11-24: This might be broken as website has ben renewed! */
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, null);
        if (account.getType() == AccountType.FREE) {
            requestFileInformation(link);
            handleFree(link);
        } else {
            requestFileInformation(link);
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            String fileID = new Regex(link.getDownloadURL(), "/(\\d+)/[^/]+\\.html$").getMatch(0);
            String premiumPage = br.getRegex("\"(x-premium/\\d+)\"").getMatch(0);
            if (fileID == null || premiumPage == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/cz/" + premiumPage, "");
            String dllink = br.getRegex("class=\"wide\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("Pokud se tak nestane, <a href=\"(/stahni-.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(/stahni-rychle/\\d+/.*?\\.html)\"").getMatch(0);
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, ai);
        return ai;
    }

    private void login(final Account account, final AccountInfo ia) throws Exception {
        AccountInfo ai = ia;
        if (ia == null) {
            ai = account.getAccountInfo();
        }
        br = new Browser();
        prepBr();
        br.setFollowRedirects(true);
        br.getPage("https://www.edisk.eu/account/login");
        final Form login = br.getFormbyAction("/account/login/");
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        if (br.getURL().contains("/account/login/")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (!br.getURL().endsWith("/account/stats/")) {
            br.getPage("/account/stats/");
        }
        String availabletraffic = br.getRegex("\\(Credit:\\s*(\\d+)\\s*\\)\\s*</strong>").getMatch(0);
        if (availabletraffic != null && !"0".equals(availabletraffic)) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic + "MB"));
            account.setValid(true);
            account.setType(AccountType.PREMIUM);
            if (ia == null) {
                account.setAccountInfo(ai);
            }
        }
        if (availabletraffic == null || ai.getTrafficLeft() <= 0) {
            /* Check if credit = zero == free account - till now there is no better way to verify this!. */
            /* 20170102 If revert is needed, revert to revision: 35520 */
            account.setValid(true);
            account.setType(AccountType.FREE);
            // can free accounts download? handlePremium would indicate yes
            ai.setUnlimitedTraffic();
            if (ia == null) {
                account.setAccountInfo(ai);
            }
        }
    }

    private void prepBr() {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}